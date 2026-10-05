package fr.xalkinn.swgohmanager.controleur;

import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import fr.xalkinn.swgohmanager.service.ConsigneService;

@Controller
@RequestMapping("/consignes")
public class ConsigneControleur {
    private final ConsigneService service;
    private final fr.xalkinn.swgohmanager.service.ConsigneExportService export;
    public ConsigneControleur(ConsigneService service, fr.xalkinn.swgohmanager.service.ConsigneExportService export) { this.service=service; this.export=export; }
    @GetMapping
    public String page(@RequestParam(defaultValue="2026-09-29") String debut,
            @RequestParam(required=false) String fin,@RequestParam(required=false) List<Long> listes,
            @RequestParam(defaultValue="false") boolean rechercher,Model model) {
        var saved=service.listes();
        var selection=listes==null ? new ArrayList<Long>() : new ArrayList<>(listes);
        if(!rechercher && listes==null) for(var l:saved) selection.add(((Number)l.get("id")).longValue());
        model.addAttribute("listes",saved); model.addAttribute("selection",selection); model.addAttribute("catalogue",service.catalogue());
        LocalDate start=LocalDate.of(2026,9,29),end=LocalDate.now(java.time.ZoneId.of("Europe/Paris"));
        try {
            start=LocalDate.parse(debut); if(fin!=null && !fin.isBlank()) end=LocalDate.parse(fin);
            if(end.isBefore(start)) throw new IllegalArgumentException("La date de fin doit suivre la date de début.");
            model.addAttribute("joueurs",service.suivi(start,end,selection));
        } catch(java.time.format.DateTimeParseException e) { model.addAttribute("erreur","Date invalide."); model.addAttribute("joueurs",List.of());
        } catch(IllegalArgumentException e) { model.addAttribute("erreur",e.getMessage()); model.addAttribute("joueurs",List.of()); }
        model.addAttribute("debut",start); model.addAttribute("fin",end);
        return "consignes";
    }
    @GetMapping("/export")
    public org.springframework.http.ResponseEntity<byte[]> exporter(
            @RequestParam(defaultValue="2026-09-29") String debut,
            @RequestParam(required=false) String fin,
            @RequestParam(required=false) List<Long> listes,
            @RequestParam(defaultValue="excel") String format) throws java.io.IOException {
        LocalDate start,end;
        try {
            start=LocalDate.parse(debut);
            end=fin==null || fin.isBlank() ? LocalDate.now(java.time.ZoneId.of("Europe/Paris")) : LocalDate.parse(fin);
            if(end.isBefore(start))throw new IllegalArgumentException("Période invalide.");
        } catch(IllegalArgumentException e) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Période invalide.",e);
        }
        fr.xalkinn.swgohmanager.service.ConsigneExportService.ExportResult data;
        try { data=export.exporter(format,start,end,listes==null?List.of():listes); }
        catch(IllegalArgumentException e) { throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,e.getMessage(),e); }
        return org.springframework.http.ResponseEntity.ok()
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"consignes-"+start+"-"+end+"."+data.extension()+"\"")
            .contentType(org.springframework.http.MediaType.parseMediaType(data.contentType()))
            .body(data.content());
    }
    @PostMapping("/decision")
    public String decision(@RequestParam String debut,@RequestParam String fin,
            @RequestParam(required=false) List<Long> listes,@RequestParam int joueur,
            @RequestParam(required=false) List<String> evolutions,@RequestParam String decision,
            RedirectAttributes redirect) {
        var selection=listes==null?List.<Long>of():listes;
        try { service.decider(LocalDate.parse(debut),LocalDate.parse(fin),selection,joueur,evolutions==null?List.of():evolutions,decision);redirect.addFlashAttribute("message","Décision enregistrée."); }
        catch(IllegalArgumentException e) { redirect.addFlashAttribute("erreur",e.getMessage()); }
        redirect.addAttribute("debut",debut);redirect.addAttribute("fin",fin);redirect.addAttribute("rechercher",true);redirect.addAttribute("joueur",joueur);
        if(!selection.isEmpty())redirect.addAttribute("listes",selection);
        return "redirect:/consignes";
    }
    @PostMapping("/liste")
    public String save(@RequestParam(required=false) Long id,@RequestParam String nom,
            @RequestParam(required=false) List<String> personnages,RedirectAttributes redirect) {
        try { service.enregistrer(id,nom,personnages==null ? List.of():personnages); redirect.addFlashAttribute("message","Liste enregistrée."); }
        catch(IllegalArgumentException e) { redirect.addFlashAttribute("erreur",e.getMessage()); }
        return "redirect:/consignes";
    }
    @PostMapping("/liste/supprimer")
    public String delete(@RequestParam long id,RedirectAttributes redirect) {
        service.supprimer(id); redirect.addFlashAttribute("message","Liste supprimée."); return "redirect:/consignes";
    }
}
