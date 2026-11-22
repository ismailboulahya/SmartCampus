package ma.ensa.smartcampus.service;

import static ma.ensa.smartcampus.service.RoomService.MESSAGE_CAPACITE_OBLIGATOIRE;
import static ma.ensa.smartcampus.service.RoomService.MESSAGE_CAPACITE_POSITIVE;
import static ma.ensa.smartcampus.service.RoomService.MESSAGE_NOM_DEJA_UTILISE;
import static ma.ensa.smartcampus.service.RoomService.MESSAGE_NOM_LONGUEUR;
import static ma.ensa.smartcampus.service.RoomService.MESSAGE_NOM_OBLIGATOIRE;
import static ma.ensa.smartcampus.service.RoomService.MESSAGE_SALLE_INTROUVABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import ma.ensa.smartcampus.exception.ConflitException;
import ma.ensa.smartcampus.exception.RessourceIntrouvableException;
import ma.ensa.smartcampus.model.Room;
import ma.ensa.smartcampus.model.TypeEquipement;
import ma.ensa.smartcampus.repository.RoomRepository;
import ma.ensa.smartcampus.service.RoomService.RoomRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;

class RoomServiceTest {

    private final RoomRepository rooms = Mockito.mock(RoomRepository.class);
    private final RoomService roomService = new RoomService(rooms);

    private static Room salleExistante(long id, String nom, int capacite) {
        return new Room(nom, capacite, Set.of(TypeEquipement.PROJECTEUR));
    }

    @Test
    void creationReussie() {
        when(rooms.existsByNom("Amphi A")).thenReturn(false);
        when(rooms.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Room salle = roomService.createRoom(
                new RoomRequest("  Amphi A  ", 120, Set.of(TypeEquipement.PROJECTEUR, TypeEquipement.TABLEAU)));

        assertEquals("Amphi A", salle.getNom());
        assertEquals(120, salle.getCapacite().intValue());
        assertTrue(salle.getEquipements().containsAll(Set.of(TypeEquipement.PROJECTEUR, TypeEquipement.TABLEAU)));
        verify(rooms).save(any(Room.class));
    }

    @Test
    void creationRefuseePourUnNomVide() {
        RequeteInvalideTest.verifier(() -> roomService.createRoom(new RoomRequest("   ", 30, Set.of())),
                MESSAGE_NOM_OBLIGATOIRE);
        RequeteInvalideTest.verifier(() -> roomService.createRoom(new RoomRequest(null, 30, Set.of())),
                MESSAGE_NOM_OBLIGATOIRE);
        verify(rooms, never()).save(any(Room.class));
    }

    @Test
    void creationRefuseePourUnNomTropCourt() {
        RequeteInvalideTest.verifier(() -> roomService.createRoom(new RoomRequest("A", 30, Set.of())),
                MESSAGE_NOM_LONGUEUR);
        verify(rooms, never()).save(any(Room.class));
    }

    @Test
    void creationRefuseePourUneCapaciteInvalide() {
        RequeteInvalideTest.verifier(() -> roomService.createRoom(new RoomRequest("Salle B", 0, Set.of())),
                MESSAGE_CAPACITE_POSITIVE);
        RequeteInvalideTest.verifier(() -> roomService.createRoom(new RoomRequest("Salle B", -5, Set.of())),
                MESSAGE_CAPACITE_POSITIVE);
        RequeteInvalideTest.verifier(() -> roomService.createRoom(new RoomRequest("Salle B", null, Set.of())),
                MESSAGE_CAPACITE_OBLIGATOIRE);
        verify(rooms, never()).save(any(Room.class));
    }

    @Test
    void creationRefuseePourUnNomDejaUtilise() {
        when(rooms.existsByNom("Amphi A")).thenReturn(true);

        ConflitException erreur = assertThrows(ConflitException.class,
                () -> roomService.createRoom(new RoomRequest("Amphi A", 120, Set.of())));

        assertEquals(MESSAGE_NOM_DEJA_UTILISE, erreur.getMessage());
        verify(rooms, never()).save(any(Room.class));
    }

    @Test
    void creationTraduitUneErreurDePersistanceEnConflit() {
        when(rooms.existsByNom(any())).thenReturn(false);
        when(rooms.save(any(Room.class))).thenThrow(new DataIntegrityViolationException("contrainte"));

        ConflitException erreur = assertThrows(ConflitException.class,
                () -> roomService.createRoom(new RoomRequest("Amphi A", 120, Set.of())));

        assertEquals(MESSAGE_NOM_DEJA_UTILISE, erreur.getMessage());
    }

    @Test
    void recupereToutesLesSalles() {
        List<Room> salles = List.of(salleExistante(1L, "Amphi A", 120), salleExistante(2L, "Salle B", 30));
        when(rooms.findAll()).thenReturn(salles);

        assertEquals(2, roomService.getAllRooms().size());
    }

    @Test
    void recupereUneSalleParIdentifiant() {
        when(rooms.findById(1L)).thenReturn(Optional.of(salleExistante(1L, "Amphi A", 120)));

        assertEquals("Amphi A", roomService.getRoomById(1L).getNom());
    }

    @Test
    void salleInexistanteRetourneUneErreur() {
        when(rooms.findById(99L)).thenReturn(Optional.empty());

        RessourceIntrouvableException erreur = assertThrows(RessourceIntrouvableException.class,
                () -> roomService.getRoomById(99L));

        assertEquals(MESSAGE_SALLE_INTROUVABLE, erreur.getMessage());
    }

    @Test
    void miseAJourReussie() {
        Room existante = salleExistante(1L, "Amphi A", 120);
        when(rooms.findById(1L)).thenReturn(Optional.of(existante));
        when(rooms.existsByNomAndIdNot("Amphi B", 1L)).thenReturn(false);
        when(rooms.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Room salle = roomService.updateRoom(1L,
                new RoomRequest("Amphi B", 150, Set.of(TypeEquipement.SONORISATION)));

        assertEquals("Amphi B", salle.getNom());
        assertEquals(150, salle.getCapacite().intValue());
        assertEquals(Set.of(TypeEquipement.SONORISATION), salle.getEquipements());
    }

    @Test
    void miseAJourSansChangerDeNomEstAcceptee() {
        Room existante = salleExistante(1L, "Amphi A", 120);
        when(rooms.findById(1L)).thenReturn(Optional.of(existante));
        when(rooms.existsByNomAndIdNot("Amphi A", 1L)).thenReturn(false);
        when(rooms.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Room salle = roomService.updateRoom(1L, new RoomRequest("Amphi A", 200, Set.of()));

        assertEquals("Amphi A", salle.getNom());
        assertEquals(200, salle.getCapacite().intValue());
    }

    @Test
    void miseAJourRefuseeAvecLeNomDUneAutreSalle() {
        when(rooms.findById(1L)).thenReturn(Optional.of(salleExistante(1L, "Amphi A", 120)));
        when(rooms.existsByNomAndIdNot("Salle B", 1L)).thenReturn(true);

        ConflitException erreur = assertThrows(ConflitException.class,
                () -> roomService.updateRoom(1L, new RoomRequest("Salle B", 150, Set.of())));

        assertEquals(MESSAGE_NOM_DEJA_UTILISE, erreur.getMessage());
        verify(rooms, never()).save(any(Room.class));
    }

    @Test
    void miseAJourDUneSalleInexistanteRetourneUneErreur() {
        when(rooms.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RessourceIntrouvableException.class,
                () -> roomService.updateRoom(99L, new RoomRequest("Salle B", 30, Set.of())));
        verify(rooms, never()).save(any(Room.class));
    }

    @Test
    void suppressionReussie() {
        when(rooms.existsById(1L)).thenReturn(true);

        roomService.deleteRoom(1L);

        verify(rooms).deleteById(1L);
    }

    @Test
    void suppressionDUneSalleInexistanteRetourneUneErreur() {
        when(rooms.existsById(99L)).thenReturn(false);

        RessourceIntrouvableException erreur = assertThrows(RessourceIntrouvableException.class,
                () -> roomService.deleteRoom(99L));

        assertEquals(MESSAGE_SALLE_INTROUVABLE, erreur.getMessage());
        verify(rooms, never()).deleteById(any());
    }

    @Test
    void suppressionRefuseeLorsqueLaSalleEstEncoreUtilisee() {
        when(rooms.existsById(1L)).thenReturn(true);
        Mockito.doThrow(new DataIntegrityViolationException("contrainte")).when(rooms).deleteById(1L);

        assertThrows(ConflitException.class, () -> roomService.deleteRoom(1L));
    }

    /** Petite aide de lecture pour les cas où seule l'exception compte. */
    private static final class RequeteInvalideTest {

        private RequeteInvalideTest() {
        }

        static void verifier(org.junit.jupiter.api.function.Executable appel, String messageAttendu) {
            ma.ensa.smartcampus.exception.RequeteInvalideException erreur =
                    assertThrows(ma.ensa.smartcampus.exception.RequeteInvalideException.class, appel);
            assertEquals(messageAttendu, erreur.getMessage());
        }
    }
}
