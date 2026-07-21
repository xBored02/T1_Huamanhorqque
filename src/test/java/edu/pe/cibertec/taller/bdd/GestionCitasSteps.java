package edu.pe.cibertec.taller.bdd;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import edu.pe.cibertec.taller.excepcion.HorarioOcupadoException;
import edu.pe.cibertec.taller.modelo.Cita;
import edu.pe.cibertec.taller.modelo.EstadoCita;
import edu.pe.cibertec.taller.modelo.Mecanico;
import edu.pe.cibertec.taller.modelo.TipoServicio;
import edu.pe.cibertec.taller.repositorio.RepositorioCitas;
import edu.pe.cibertec.taller.repositorio.RepositorioMecanicos;
import edu.pe.cibertec.taller.servicio.impl.ServicioCitasImpl;
import edu.pe.cibertec.taller.util.ProveedorFechaHora;
import edu.pe.cibertec.taller.util.ServicioNotificaciones;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class GestionCitasSteps {

	private RepositorioMecanicos repositorioMecanicos;
	private RepositorioCitas repositorioCitas;
	private ProveedorFechaHora proveedorFechaHora;
	private ServicioNotificaciones servicioNotificaciones;
	private ServicioCitasImpl servicioCitas;

	@Before
	public void inicializar() {
		repositorioMecanicos = mock(RepositorioMecanicos.class);
		repositorioCitas = mock(RepositorioCitas.class);
		proveedorFechaHora = mock(ProveedorFechaHora.class);
		servicioNotificaciones = mock(ServicioNotificaciones.class);
		servicioCitas = new ServicioCitasImpl(repositorioMecanicos, repositorioCitas,
				proveedorFechaHora, servicioNotificaciones);
	}

	private Exception excepcion;
	private LocalDate dia = LocalDate.of(2026, 9, 13);

	@Given("que el mecanico {long} tiene una cita programada de 10:00 a 12:00")
	public void mecanicoConCitaProgramada(Long idMecanico) {
		Mecanico mecanico = new Mecanico();
		mecanico.setId(idMecanico);
		mecanico.setEspecialidad(TipoServicio.MANTENIMIENTO_LIGERO);

		Cita citaAnterior = new Cita();
		citaAnterior.setId(999L);
		citaAnterior.setFechaHoraInicio(dia.atTime(10, 0));
		citaAnterior.setDuracionHoras(2);
		citaAnterior.setTipoServicio(TipoServicio.MANTENIMIENTO_LIGERO);
		citaAnterior.setEstado(EstadoCita.PROGRAMADA);

		when(repositorioMecanicos.findById(anyLong())).thenReturn(Optional.of(mecanico));
		when(proveedorFechaHora.ahora()).thenReturn(dia.atTime(8, 0));

		when(repositorioCitas.findByMecanicoIdAndEstado(anyLong(), any()))
				.thenReturn(List.of(citaAnterior));
	}

	@Given("un mecanico {long} esta disponible")
	public void mecanicoDisponible(Long idMecanico) {
		Mecanico mecanico2 = new Mecanico();
		mecanico2.setId(idMecanico);
		mecanico2.setEspecialidad(TipoServicio.MANTENIMIENTO_LIGERO);

		when(repositorioMecanicos.findById(idMecanico)).thenReturn(Optional.of(mecanico2));
		when(repositorioCitas.findByMecanicoIdAndEstado(idMecanico, EstadoCita.PROGRAMADA))
				.thenReturn(List.of());
	}

	@When("agendo un {string} a las {int}:{int} para la placa {string} con el mecanico {long}")
	public void agendarCita(String tipo, int hora, int minuto, String placa, Long idMecanico) {
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));

		try {
			LocalDateTime fechaCita = dia.atTime(hora, minuto);
			servicioCitas.agendarCita(idMecanico, placa, TipoServicio.valueOf(tipo), fechaCita);
		} catch (Exception e) {
			this.excepcion = e;
		}
	}

	@Then("la cita queda programada")
	public void verificarCitaProgramada() {
		assertNull(excepcion, "No debio lanzar ninguna excepcion al guardar");
		verify(repositorioCitas, times(1)).save(any(Cita.class));
	}

	@Then("se notifica al cliente")
	public void verificarNotificacion() {
		verify(servicioNotificaciones, times(1)).notificarCitaAgendada(any(Cita.class));
	}

	@Then("el sistema arroja la excepcion de horario ocupado")
	public void verificarExcepcionHorarioOcupado() {
		assertNotNull(excepcion, "Debio lanzar una excepcion por el cruce de horarios");
		assertTrue(excepcion instanceof HorarioOcupadoException, "La excepcion debe ser HorarioOcupadoException");
		verify(repositorioCitas, never()).save(any(Cita.class));
	}
}

