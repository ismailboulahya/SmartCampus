package ma.ensa.smartcampus.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import ma.ensa.smartcampus.exception.ConflitException;
import ma.ensa.smartcampus.exception.RequeteInvalideException;
import ma.ensa.smartcampus.exception.RessourceIntrouvableException;
import ma.ensa.smartcampus.model.Room;
import ma.ensa.smartcampus.model.TypeEquipement;
import ma.ensa.smartcampus.repository.RoomRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Gestion des salles : lecture, création, mise à jour et suppression.
 */
@Service
public class RoomService {

    public static final String MESSAGE_NOM_OBLIGATOIRE = "Le nom de la salle est obligatoire.";
    public static final String MESSAGE_NOM_LONGUEUR =
            "Le nom de la salle doit contenir entre 2 et 100 caractères.";
    public static final String MESSAGE_CAPACITE_OBLIGATOIRE = "La capacité de la salle est obligatoire.";
    public static final String MESSAGE_CAPACITE_POSITIVE =
            "La capacité de la salle doit être strictement positive.";
    public static final String MESSAGE_NOM_DEJA_UTILISE = "Une salle porte déjà ce nom.";
    public static final String MESSAGE_SALLE_INTROUVABLE = "Salle introuvable.";
    public static final String MESSAGE_DONNEES_OBLIGATOIRES =
            "Les informations de la salle sont obligatoires.";
    public static final String MESSAGE_SUPPRESSION_IMPOSSIBLE =
            "La salle ne peut pas être supprimée car elle est encore utilisée.";

    private static final int LONGUEUR_MINIMALE_DU_NOM = 2;
    private static final int LONGUEUR_MAXIMALE_DU_NOM = 100;

    public record RoomRequest(
            @NotBlank(message = MESSAGE_NOM_OBLIGATOIRE)
            @Size(min = LONGUEUR_MINIMALE_DU_NOM, max = LONGUEUR_MAXIMALE_DU_NOM, message = MESSAGE_NOM_LONGUEUR)
            String nom,
            @NotNull(message = MESSAGE_CAPACITE_OBLIGATOIRE)
            @Positive(message = MESSAGE_CAPACITE_POSITIVE)
            Integer capacite,
            Set<TypeEquipement> equipements) {
    }

    private final RoomRepository rooms;

    public RoomService(RoomRepository rooms) {
        this.rooms = rooms;
    }

    public List<Room> getAllRooms() {
        return rooms.findAll();
    }

    public Room getRoomById(Long id) {
        if (id == null) {
            throw new RequeteInvalideException(MESSAGE_DONNEES_OBLIGATOIRES);
        }
        Optional<Room> salle = rooms.findById(id);
        if (salle.isEmpty()) {
            throw new RessourceIntrouvableException(MESSAGE_SALLE_INTROUVABLE);
        }
        return salle.get();
    }

    public Room createRoom(RoomRequest request) {
        String nom = validerEtNormaliser(request);
        if (rooms.existsByNom(nom)) {
            throw new ConflitException(MESSAGE_NOM_DEJA_UTILISE);
        }
        try {
            return rooms.save(new Room(nom, request.capacite(), equipementsDe(request)));
        } catch (DataIntegrityViolationException exception) {
            // La contrainte d'unicité de la base protège contre une création concurrente.
            throw new ConflitException(MESSAGE_NOM_DEJA_UTILISE);
        }
    }

    public Room updateRoom(Long id, RoomRequest request) {
        if (id == null) {
            throw new RequeteInvalideException(MESSAGE_DONNEES_OBLIGATOIRES);
        }
        Room salle = getRoomById(id);
        String nom = validerEtNormaliser(request);
        if (rooms.existsByNomAndIdNot(nom, id)) {
            throw new ConflitException(MESSAGE_NOM_DEJA_UTILISE);
        }
        salle.setNom(nom);
        salle.setCapacite(request.capacite());
        salle.setEquipements(equipementsDe(request));
        try {
            return rooms.save(salle);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflitException(MESSAGE_NOM_DEJA_UTILISE);
        }
    }

    public void deleteRoom(Long id) {
        if (id == null) {
            throw new RequeteInvalideException(MESSAGE_DONNEES_OBLIGATOIRES);
        }
        if (!rooms.existsById(id)) {
            throw new RessourceIntrouvableException(MESSAGE_SALLE_INTROUVABLE);
        }
        try {
            rooms.deleteById(id);
        } catch (DataIntegrityViolationException exception) {
            // Une salle référencée par d'autres données ne peut pas être supprimée.
            throw new ConflitException(MESSAGE_SUPPRESSION_IMPOSSIBLE);
        }
    }

    private String validerEtNormaliser(RoomRequest request) {
        if (request == null) {
            throw new RequeteInvalideException(MESSAGE_DONNEES_OBLIGATOIRES);
        }
        String nom = request.nom() == null ? null : request.nom().trim();
        if (nom == null || nom.isEmpty()) {
            throw new RequeteInvalideException(MESSAGE_NOM_OBLIGATOIRE);
        }
        if (nom.length() < LONGUEUR_MINIMALE_DU_NOM || nom.length() > LONGUEUR_MAXIMALE_DU_NOM) {
            throw new RequeteInvalideException(MESSAGE_NOM_LONGUEUR);
        }
        if (request.capacite() == null) {
            throw new RequeteInvalideException(MESSAGE_CAPACITE_OBLIGATOIRE);
        }
        if (request.capacite() <= 0) {
            throw new RequeteInvalideException(MESSAGE_CAPACITE_POSITIVE);
        }
        return nom;
    }

    private static Set<TypeEquipement> equipementsDe(RoomRequest request) {
        Set<TypeEquipement> equipements = new LinkedHashSet<>();
        if (request.equipements() != null) {
            request.equipements().stream().filter(Objects::nonNull).forEach(equipements::add);
        }
        return equipements;
    }
}
