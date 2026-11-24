package ma.ensa.smartcampus.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import ma.ensa.smartcampus.model.Room;
import ma.ensa.smartcampus.model.TypeEquipement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vérifie la persistance des salles, de leurs équipements et de la contrainte d'unicité.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoomRepositoryTest {

    @Autowired
    private RoomRepository rooms;

    @Test
    void enregistreUneSalleAvecSesEquipements() {
        Room salle = rooms.saveAndFlush(
                new Room("Amphi Test Equipements", 80, Set.of(TypeEquipement.PROJECTEUR, TypeEquipement.TABLEAU)));

        Room relue = rooms.findById(salle.getId()).orElseThrow();
        assertEquals("Amphi Test Equipements", relue.getNom());
        assertEquals(80, relue.getCapacite().intValue());
        assertEquals(2, relue.getEquipements().size());
    }

    @Test
    void refuseDeuxSallesAvecLeMemeNom() {
        rooms.saveAndFlush(new Room("Salle Nom Unique", 40, Set.of()));

        assertThrows(DataIntegrityViolationException.class,
                () -> rooms.saveAndFlush(new Room("Salle Nom Unique", 25, Set.of())));
    }

    @Test
    void detecteLeNomDuneAutreSalleUniquement() {
        Room premiere = rooms.saveAndFlush(new Room("Salle Identite A", 40, Set.of()));
        Room seconde = rooms.saveAndFlush(new Room("Salle Identite B", 40, Set.of()));

        assertTrue(rooms.existsByNom("Salle Identite A"));
        assertTrue(rooms.existsByNomAndIdNot("Salle Identite A", seconde.getId()));
        assertFalse(rooms.existsByNomAndIdNot("Salle Identite A", premiere.getId()));
    }
}
