Feature: Gestion de citas del taller mecanico

  Scenario: Registro exitoso de MANTENIMIENTO_LIGERO con otro mecanico
    Given que el mecanico 1 tiene una cita programada de 10:00 a 12:00
    And un mecanico 2 esta disponible
    When agendo un "MANTENIMIENTO_LIGERO" a las 10:00 para la placa "HUA-573" con el mecanico 2
    Then la cita queda programada
    And se notifica al cliente

  Scenario: Intento con el mecanico ocupado iniciando a las 11:00
    Given que el mecanico 1 tiene una cita programada de 10:00 a 12:00
    When agendo un "MANTENIMIENTO_LIGERO" a las 11:00 para la placa "HUA-573" con el mecanico 1
    Then el sistema arroja la excepcion de horario ocupado

  Scenario: Intento con el mecanico ocupado iniciando a las 12:00
    Given que el mecanico 1 tiene una cita programada de 10:00 a 12:00
    When agendo un "MANTENIMIENTO_LIGERO" a las 12:00 para la placa "HUA-573" con el mecanico 1
    Then la cita queda programada
