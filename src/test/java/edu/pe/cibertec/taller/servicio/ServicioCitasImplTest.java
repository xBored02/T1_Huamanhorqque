package edu.pe.cibertec.taller.servicio;

import edu.pe.cibertec.taller.excepcion.CitaNoCancelableException;
import edu.pe.cibertec.taller.excepcion.EspecialidadIncorrectaException;
import edu.pe.cibertec.taller.excepcion.HorarioNoPermitidoException;
import edu.pe.cibertec.taller.excepcion.MecanicoNoEncontradoException;
import edu.pe.cibertec.taller.modelo.*;
import edu.pe.cibertec.taller.repositorio.RepositorioCitas;
import edu.pe.cibertec.taller.repositorio.RepositorioMecanicos;
import edu.pe.cibertec.taller.servicio.impl.ServicioCitasImpl;
import edu.pe.cibertec.taller.util.ProveedorFechaHora;
import edu.pe.cibertec.taller.util.ServicioNotificaciones;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioCitasImplTest {

	@Mock
	private RepositorioMecanicos repositorioMecanicos;

	@Mock
	private RepositorioCitas repositorioCitas;

	@Mock
	private ProveedorFechaHora proveedorFechaHora;

	@Mock
	private ServicioNotificaciones servicioNotificaciones;

	private ServicioCitasImpl servicioCitas;

	@BeforeEach
	void inicializar() {
		servicioCitas = new ServicioCitasImpl(repositorioMecanicos, repositorioCitas,
				proveedorFechaHora, servicioNotificaciones);

	}

	@Test
	@DisplayName("Agendar una cita valida la guarda, notifica y la retorna en estado PROGRAMADA")
	void agendarCitaExitosa() {

		// Arrange
		Long idMecanico = 1L;
		Mecanico mecanico = new Mecanico();
		mecanico.setId(idMecanico);
		mecanico.setNombre("Jubert Huamanhorqque");
		mecanico.setEspecialidad(TipoServicio.CAMBIO_ACEITE);
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, 13, 10, 0);

		when(repositorioMecanicos.findById(idMecanico)).thenReturn(Optional.of(mecanico));
		when(proveedorFechaHora.ahora()).thenReturn(LocalDateTime.of(2026, 9, 12, 8, 0));
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

		// Act

		Cita resultado = servicioCitas.agendarCita(idMecanico, "HUA-573", TipoServicio.CAMBIO_ACEITE, fechaCita);

		// Assert
		// TODO: verificar estado, duracion, save y notificacion

		assertNotNull(resultado);
		assertEquals(EstadoCita.PROGRAMADA, resultado.getEstado());
		assertEquals(TipoServicio.CAMBIO_ACEITE.getDuracionHoras(), resultado.getDuracionHoras());
		verify(repositorioCitas, times(1)).save(any(Cita.class));
		verify(servicioNotificaciones, times(1)).notificarCitaAgendada(any(Cita.class));

	}

	@Test
	@DisplayName("Agendar con un mecanico inexistente lanza MecanicoNoEncontradoException")
	void agendarConMecanicoInexistente() {

		// Arrange
		Long idMecanico = 99L;
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, 13, 10, 0);

		when(repositorioMecanicos.findById(idMecanico)).thenReturn(Optional.empty());

		// Act y Assert
		assertThrows(MecanicoNoEncontradoException.class, () -> {
			servicioCitas.agendarCita(idMecanico, "HUA-573", TipoServicio.CAMBIO_ACEITE, fechaCita);
		});

		// Verificar que nada se guardó ni se notificó
		verify(repositorioCitas, never()).save(any(Cita.class));
		verify(servicioNotificaciones, never()).notificarCitaAgendada(any(Cita.class));

	}

	@Test
	@DisplayName("Agendar cuando la especialidad no coincide lanza EspecialidadIncorrectaException")
	void agendarConEspecialidadIncorrecta() {
		// Arrange
		Long idMecanico = 1L;
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, 13, 10, 0);

		Mecanico mecanico = new Mecanico();
		mecanico.setId(idMecanico);
		mecanico.setEspecialidad(TipoServicio.CAMBIO_ACEITE);

		when(repositorioMecanicos.findById(idMecanico)).thenReturn(Optional.of(mecanico));

		// Act y Assert
		assertThrows(EspecialidadIncorrectaException.class, () -> {
			servicioCitas.agendarCita(idMecanico, "HUA-573", TipoServicio.REPARACION_MOTOR, fechaCita);
		});

		// Verificar que nada se guardó ni se notificó
		verify(repositorioCitas, never()).save(any(Cita.class));
		verify(servicioNotificaciones, never()).notificarCitaAgendada(any(Cita.class));
	}

	//Según las reglas de negocio en el README el horario de atención
	//a los servicios pesados es entre 8 AM y hasta antes de las 12 PM
	//La pregunta manda 7, 8 , 11 y 12
	// 7 y 12 rechazados; 8 y 11 aceptados

	@Test
	@DisplayName("Un servicio pesado a las 07:00 se rechaza con HorarioNoPermitidoException")
	void agendarServicioPesado7Am() {
		// Arrange
		Long idMecanico = 1L;
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, 13, 7, 0);

		Mecanico mecanico = new Mecanico();
		mecanico.setId(idMecanico);
		mecanico.setEspecialidad(TipoServicio.REPARACION_MOTOR);

		when(repositorioMecanicos.findById(idMecanico)).thenReturn(Optional.of(mecanico));

		// Act y Assert
		assertThrows(HorarioNoPermitidoException.class, () -> {
			servicioCitas.agendarCita(idMecanico, "HUA-573", TipoServicio.REPARACION_MOTOR, fechaCita);
		});
		verify(repositorioCitas, never()).save(any(Cita.class));
	}

	@Test
	@DisplayName("Un servicio pesado a las 08:00 se acepta y se guarda")
	void agendarServicioPesado8Am() {
		// Arrange
		Long idMecanico = 1L;
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, 13, 8, 0);

		Mecanico mecanico = new Mecanico();
		mecanico.setId(idMecanico);
		mecanico.setEspecialidad(TipoServicio.REPARACION_MOTOR);

		when(repositorioMecanicos.findById(idMecanico)).thenReturn(Optional.of(mecanico));
		when(proveedorFechaHora.ahora()).thenReturn(LocalDateTime.of(2026, 9, 12, 8, 0));

		// Act y Assert

		assertDoesNotThrow(() -> {
			servicioCitas.agendarCita(idMecanico, "HUA-573", TipoServicio.REPARACION_MOTOR, fechaCita);
		});
		verify(repositorioCitas, times(1)).save(any(Cita.class));
	}

	@Test
	@DisplayName("Un servicio pesado a las 11:00 se acepta y se guarda")
	void agendarServicioPesado11Am() {
		// Arrange
		Long idMecanico = 1L;
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, 13, 11, 0);

		Mecanico mecanico = new Mecanico();
		mecanico.setId(idMecanico);
		mecanico.setEspecialidad(TipoServicio.REPARACION_MOTOR);

		when(repositorioMecanicos.findById(idMecanico)).thenReturn(Optional.of(mecanico));
		when(proveedorFechaHora.ahora()).thenReturn(LocalDateTime.of(2026, 9, 12, 8, 0));

		// Act y Assert

		assertDoesNotThrow(() -> {
			servicioCitas.agendarCita(idMecanico, "HUA-573", TipoServicio.REPARACION_MOTOR, fechaCita);
		});
		verify(repositorioCitas, times(1)).save(any(Cita.class));
	}

	@Test
	@DisplayName("Un servicio pesado a las 12:00 se rechaza con HorarioNoPermitidoException")
	void agendarServicioPesado12Pm() {
		// Arrange
		Long idMecanico = 1L;
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, 13, 12, 0);

		Mecanico mecanico = new Mecanico();
		mecanico.setId(idMecanico);
		mecanico.setEspecialidad(TipoServicio.REPARACION_MOTOR);

		when(repositorioMecanicos.findById(idMecanico)).thenReturn(Optional.of(mecanico));

		// Act y Assert
		assertThrows(HorarioNoPermitidoException.class, () -> {
			servicioCitas.agendarCita(idMecanico, "HUA-573", TipoServicio.REPARACION_MOTOR, fechaCita);
		});
		verify(repositorioCitas, never()).save(any(Cita.class));
	}
//
//	@Test
//	@DisplayName("Agendar en una fecha del pasado lanza FechaInvalidaException")
//	void agendarConFechaEnElPasado() {
//		// Arrange
//		// TODO: recuerden mockear proveedorFechaHora.ahora()
//
//		// Act y Assert
//		// TODO
//	}
//
//	@Test
//	@DisplayName("Agendar sobre una cita ya programada se rechaza con HorarioOcupadoException")
//	void agendarConSuperposicion() {
//		// Arrange
//		// TODO
//
//		// Act y Assert
//		// TODO
//	}
//
//	@Test
//	@DisplayName("Una cita que empieza justo cuando termina otra se acepta")
//	void agendarCitaContigua() {
//		// Arrange
//		// TODO: una cita existente que termina a las 10:00 y la nueva que empieza a las 10:00
//
//		// Act
//		// TODO
//
//		// Assert
//		// TODO
//	}

	@Test
	@DisplayName("Cancelar con 24 horas o mas de anticipacion no genera penalidad")
	void cancelarConAnticipacionSuficiente() {
		// Arrange
		Long idCita = 1L;
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, 13, 10, 0);

		Cita cita = new Cita();
		cita.setId(idCita);
		cita.setPlacaVehiculo("HUA-573");
		cita.setEstado(EstadoCita.PROGRAMADA);
		cita.setFechaHoraInicio(fechaCita);

		when(repositorioCitas.findById(idCita)).thenReturn(Optional.of(cita));
		when(proveedorFechaHora.ahora()).thenReturn(LocalDateTime.of(2026, 9, 12, 8, 0));

		// Act
		ResultadoCancelacion resultado = servicioCitas.cancelarCita(idCita);
		// Assert
		assertEquals(EstadoCita.CANCELADA, cita.getEstado());
		assertEquals(0.0, resultado.getMontoPenalidad());
		verify(servicioNotificaciones).notificarCitaCancelada(cita);
	}

	@Test
	@DisplayName("Cancelar con menos de 24 horas aplica una penalidad de 50.00")
	void cancelarConAvisoTardio() {
		// Arrange
		Long idCita = 1L;
		LocalDateTime fechaCita = LocalDateTime.of(2026, 9, 13, 10, 0);

		Cita cita = new Cita();
		cita.setId(idCita);
		cita.setPlacaVehiculo("HUA-573");
		cita.setEstado(EstadoCita.PROGRAMADA);
		cita.setFechaHoraInicio(fechaCita);

		when(repositorioCitas.findById(idCita)).thenReturn(Optional.of(cita));
		when(proveedorFechaHora.ahora()).thenReturn(LocalDateTime.of(2026, 9, 13, 8, 0));

		// Act
		ResultadoCancelacion resultado = servicioCitas.cancelarCita(idCita);
		// Assert
		assertEquals(50.0, resultado.getMontoPenalidad());
		assertEquals(EstadoCita.CANCELADA, cita.getEstado());
	}

//	@Test
//	@DisplayName("Cancelar una cita inexistente lanza CitaNoEncontradaException")
//	void cancelarCitaInexistente() {
//		// Arrange
//		// TODO
//
//		// Act y Assert
//		// TODO
//	}

	@Test
	@DisplayName("Cancelar una cita que ya fue atendida lanza CitaNoCancelableException")
	void cancelarCitaYaAtendida() {
		// Arrange
		Long idCita = 1L;

		Cita cita = new Cita();
		cita.setId(idCita);
		cita.setPlacaVehiculo("HUA-573");
		cita.setEstado(EstadoCita.ATENDIDA);

		when(repositorioCitas.findById(idCita)).thenReturn(Optional.of(cita));

		// Act y Assert
		assertThrows(CitaNoCancelableException.class, () -> {
			servicioCitas.cancelarCita(idCita);
		});
	}

//	@Test
//	@DisplayName("Buscar mecanico disponible retorna el primero sin citas superpuestas")
//	void buscarMecanicoDisponibleRetornaPrimeroLibre() {
//		// Arrange
//		// TODO: dos mecanicos de la misma especialidad, el primero ocupado
//
//		// Act
//		// TODO
//
//		// Assert
//		// TODO
//	}
//
//	@Test
//	@DisplayName("Buscar mecanico cuando ninguno esta libre lanza SinDisponibilidadException")
//	void buscarMecanicoSinDisponibilidad() {
//		// Arrange
//		// TODO
//
//		// Act y Assert
//		// TODO
//	}
}
