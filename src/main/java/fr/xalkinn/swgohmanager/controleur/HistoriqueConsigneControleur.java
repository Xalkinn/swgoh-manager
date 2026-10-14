package fr.xalkinn.swgohmanager.controleur;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import fr.xalkinn.swgohmanager.service.*;
@Controller
@RequestMapping("/historique-consignes")
public class HistoriqueConsigneControleur {
    private final HistoriqueConsigneService service;
    private final ConsigneService consignes;
    private final HistoriqueConsigneExportService exports;
    public HistoriqueConsigneControleur(HistoriqueConsigneService service,ConsigneService consignes,HistoriqueConsigneExportService exports){this.service=service;this.consignes=consignes;this.exports=exports;}
    @GetMapping
    public String page(@RequestParam(defaultValue="2026-09-29") String debut,@RequestParam(required=false) String fin,
            @RequestParam(required=false) Long campagne,@RequestParam(defaultValue="") String joueur,
            @RequestParam(defaultValue="") String personnage,@RequestParam(defaultValue="") String statut,Model model) {
        LocalDate start=LocalDate.of(2026,9,29),end=HistoriqueConsigneService.maintenant().toLocalDate();
        List<Map<String,Object>> events=List.of();
        try{start=LocalDate.parse(debut);if(fin!=null&&!fin.isBlank())end=LocalDate.parse(fin);events=service.evenements(start,end,campagne,joueur,personnage,statut);}
        catch(IllegalArgumentException e){model.addAttribute("erreur","Filtres invalides : vérifie les dates et le statut.");}
        model.addAttribute("debut",start);model.addAttribute("fin",end);model.addAttribute("campagne",campagne);model.addAttribute("joueur",joueur);model.addAttribute("personnage",personnage);model.addAttribute("statut",statut);
        model.addAttribute("evenements",events);model.addAttribute("resume",service.resume(events));model.addAttribute("campagnes",service.campagnes());model.addAttribute("listes",consignes.listes());model.addAttribute("erreurSync",service.erreur());
        model.addAttribute("nouveauDebut",service.campagnes().isEmpty()?LocalDateTime.of(2026,9,29,0,0):HistoriqueConsigneService.maintenant().withNano(0));
        return "historique-consignes";
    }
    @PostMapping("/campagne")
    public String creer(@RequestParam String nom,@RequestParam String debut,@RequestParam(required=false) List<Long> listes,RedirectAttributes r){
        try{service.creer(nom,LocalDateTime.parse(debut),listes==null?List.of():listes);r.addFlashAttribute("message","Campagne créée. Clique sur Actualiser l’historique pour importer les upgrades disponibles.");}
        catch(IllegalArgumentException e){r.addFlashAttribute("erreur",e.getMessage());}return "redirect:/historique-consignes";
    }
    @PostMapping("/cloturer")
    public String cloturer(@RequestParam long id,RedirectAttributes r){try{service.cloturer(id);r.addFlashAttribute("message","Campagne clôturée. Son historique reste consultable.");}catch(IllegalArgumentException e){r.addFlashAttribute("erreur",e.getMessage());}return "redirect:/historique-consignes";}
    @PostMapping("/actualiser")
    public String actualiser(RedirectAttributes r){try{int n=service.synchroniser();r.addFlashAttribute("message",n+" nouvelle(s) évolution(s) archivée(s).");}catch(RuntimeException e){r.addFlashAttribute("erreur","Actualisation impossible. Consulte le journal de l’application.");}return "redirect:/historique-consignes";}
    @PostMapping("/decision")
    public String decider(@RequestParam long id,@RequestParam String decision,@RequestParam(defaultValue="") String motif,
            @RequestParam String debut,@RequestParam String fin,@RequestParam(required=false) Long campagne,
            @RequestParam(defaultValue="") String joueur,@RequestParam(defaultValue="") String personnage,@RequestParam(defaultValue="") String statut,RedirectAttributes r){
        try{service.decider(id,decision,motif);r.addFlashAttribute("message","Décision enregistrée.");}catch(IllegalArgumentException e){r.addFlashAttribute("erreur",e.getMessage());}
        r.addAttribute("debut",debut);r.addAttribute("fin",fin);if(campagne!=null)r.addAttribute("campagne",campagne);r.addAttribute("joueur",joueur);r.addAttribute("personnage",personnage);r.addAttribute("statut",statut);return "redirect:/historique-consignes";
    }
    @GetMapping("/export")
    public org.springframework.http.ResponseEntity<byte[]> exporter(@RequestParam(defaultValue="excel") String format,
            @RequestParam String debut,@RequestParam String fin,@RequestParam(required=false) Long campagne,
            @RequestParam(defaultValue="") String joueur,@RequestParam(defaultValue="") String personnage,@RequestParam(defaultValue="") String statut)throws java.io.IOException {
        try {
            var events=service.evenements(LocalDate.parse(debut),LocalDate.parse(fin),campagne,joueur,personnage,statut);
            var result=exports.exporter(format,events,service.resume(events),"Du "+debut+" au "+fin+" | Campagne : "+(campagne==null?"toutes":campagne)+" | Joueur : "+joueur+" | Personnage : "+personnage+" | Statut : "+statut);
            return org.springframework.http.ResponseEntity.ok().header("Content-Disposition","attachment; filename=\"historique-consignes-"+debut+"-"+fin+"."+result.extension()+"\"").contentType(org.springframework.http.MediaType.parseMediaType(result.contentType())).body(result.content());
        }catch(IllegalArgumentException e){throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,e.getMessage(),e);}
    }
}
