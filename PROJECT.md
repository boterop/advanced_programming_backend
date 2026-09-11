# Programación Avanzada
### Programa de Ingeniería de Sistemas y Computación — Universidad del Quindío

**Título:** Proyecto Final del espacio académico — SGA: Sistema de Gestión de Alojamiento
**Duración estimada:** 60 horas
**Docente:** Jhan Carlos Martinez Ceballos
**Modalidad:** equipos de trabajo, desarrollo incremental durante todo el semestre
**Versión del documento:** 2.0

---

# 1. Objetivo

Desarrollar una aplicación web para la gestión integral de un alojamiento turístico compuesto por apartamentos autónomos, cubriendo inventario, tarifas por temporada, disponibilidad, reservas, operación diaria, cobros y venta multicanal, con roles diferenciados, aplicando **arquitectura hexagonal** con **Spring Boot** en el backend y **React** en el frontend.

El sistema se construye de forma **incremental a lo largo del semestre**: cada guía del curso agrega una capa al mismo producto, en el orden dominio → servicios de aplicación → infraestructura → presentación → integraciones.

---

# 2. Contexto del negocio

## 2.1 Naturaleza del alojamiento

- Un negocio turístico compuesto por un conjunto de **apartamentos independientes y autosuficientes**, ubicados en una misma edificación o conjunto.
- Cada apartamento cuenta con sus propios dormitorios, cocina, baño y zona social.
- Las áreas comunes de la edificación (parqueadero, terraza, accesos) **no son inventario vendible**.
- **El sistema administra un único alojamiento.** No es un marketplace ni una plataforma multi-propietario.

## 2.2 Unidad de venta

- La unidad de venta es el **apartamento completo**, entregado en exclusiva a un grupo de huéspedes.
- Los dormitorios **no se venden por separado**: son una característica descriptiva del apartamento, no un producto.
- La unidad temporal de venta es la **noche-apartamento**.

## 2.3 Modelo de cobro

- Se cobra **por persona, por noche**. El valor depende de cuántos ocupantes lleguen, no solo del apartamento.
- Existe un **umbral de edad facturable**: los ocupantes menores a esa edad **ocupan cupo pero no generan cargo**. Capacidad y facturación son dos cuentas distintas sobre las mismas personas.
- La tarifa varía según la **temporada** del calendario. Una misma estancia puede cruzar varias temporadas y se liquida **noche por noche**.
- El cobro se registra en un **folio** por reserva, que admite **varios pagos parciales** y **distintos medios de pago**.

## 2.4 Canales de venta

- El alojamiento vende por **tres canales** sobre el mismo inventario físico:
  - **Portal:** el huésped reserva por sí mismo desde la aplicación.
  - **Directo:** la recepción registra una reserva recibida por teléfono, mensajería o presencialmente.
  - **Externo:** una plataforma de terceros envía la reserva al sistema por integración.

## 2.5 Riesgo central del negocio

- **Vender dos veces la misma noche del mismo apartamento.** Esta es la razón de existir del sistema y el origen de sus reglas más importantes.
- Adicionalmente: cotizaciones inconsistentes al cambiar la composición del grupo, apartamentos entregados sin preparación terminada, saldos sin trazabilidad, y cancelaciones sin registro de la política aplicada.

## 2.6 Origen del caso

El contexto se levantó a partir de la operación real de un apartahotel en Salento, Quindío. **El proyecto no es ese negocio**: cada equipo define su propio alojamiento (ver sección 6 y Anexo A). Lo que se comparte entre equipos es la lógica del negocio, no los datos.

---

# 3. Definiciones operativas

Esta sección elimina las ambigüedades que, de no resolverse, generan implementaciones divergentes y discusiones en la sustentación. **Su cumplimiento es obligatorio y evaluable.**

## 3.1 Fechas, noches y solapamiento

- Una **estancia** se expresa con dos fechas sin hora: **fecha de entrada** y **fecha de salida**.
- El intervalo es **cerrado en la entrada y abierto en la salida**: `[entrada, salida)`. La noche de la fecha de salida **no se cobra ni se ocupa**.
- **Número de noches** = días calendario entre la fecha de entrada y la fecha de salida. Del 10 al 12 son **dos** noches: la del 10 y la del 11.
- **Dos estancias se solapan** si comparten al menos una noche, es decir, si `entrada_A < salida_B` y `entrada_B < salida_A`.
- Las **horas de entrada y salida** son información operativa para la recepción. **No intervienen en el cálculo de disponibilidad**, salvo por el tiempo de preparación (sección 3.3).
- Todas las fechas se manejan en la zona horaria de Colombia.

## 3.2 Ocupantes, edad y capacidad

- De cada ocupante se registra su **fecha de nacimiento**, no su edad. La edad se calcula, nunca se almacena.
- Un ocupante es **facturable** si, **a la fecha de entrada de la estancia**, su edad alcanza o supera el umbral configurado. Un ocupante que cumple años durante la estancia no cambia de condición a mitad de camino.
- **Todos los ocupantes cuentan para la capacidad**, sean facturables o no.
- El **titular** es siempre un ocupante facturable de la reserva.

## 3.3 Disponibilidad, estado operativo y bloqueos

Son tres conceptos distintos y no deben confundirse:

| Concepto | Naturaleza | Responde a |
|---|---|---|
| **Disponibilidad** | Cálculo sobre un rango de fechas | ¿Puedo vender estas noches? |
| **Estado operativo** | Condición física presente del apartamento | ¿Puedo entregar este apartamento ahora? |
| **Bloqueo** | Registro con rango de fechas y motivo | ¿Hay una decisión administrativa que impide vender? |

- Un apartamento está **disponible** para un rango si, en todas las noches de ese rango: no tiene reservas activas que solapen, no tiene bloqueos que solapen, está activo, y su capacidad alcanza para el grupo.
- Los **estados operativos** son: `PREPARADO`, `OCUPADO`, `PENDIENTE_PREPARACION`, `EN_PREPARACION`, `FUERA_DE_SERVICIO`.
- El **tiempo de preparación** se expresa en horas y se aplica entre la hora de salida y la hora de entrada del mismo día. Si el tiempo configurado **excede** la ventana entre ambas horas, el apartamento **no puede recibir una entrada el mismo día de una salida** y esa noche queda fuera de la disponibilidad.
- Un bloqueo **no** cambia por sí solo el estado operativo, ni al revés. Un apartamento `FUERA_DE_SERVICIO` sin bloqueo registrado sigue apareciendo como vendible a futuro: por eso el administrador debe registrar el bloqueo.

## 3.4 Dinero

- Moneda única: **peso colombiano (COP)**, sin decimales.
- Todo valor monetario se representa con un tipo de precisión exacta. **Prohibido usar `float` o `double`.**
- Redondeo al peso más cercano, aplicado **al final** del cálculo de cada cargo, nunca noche por noche.
- **El sistema registra movimientos de dinero; no los ejecuta.** Una devolución se calcula y se deja registrada; el desembolso ocurre fuera del sistema.

## 3.5 Congelamiento del valor y de las políticas

- Al crearse una reserva, el sistema **congela**: el valor calculado con su desglose por noche, y la versión de la política de cancelación vigente en ese momento.
- Un cambio posterior de tarifas o de política **no altera** las reservas ya creadas.
- Solo una **modificación explícita** de la reserva (fechas, apartamento u ocupantes) recalcula el valor, y lo hace con las tarifas vigentes al momento de la modificación. La diferencia se registra como un **ajuste en el folio**.

## 3.6 Persona y usuario

- **Titular** y **ocupante** son conceptos del negocio: existen aunque nunca usen el sistema.
- **Usuario** es un concepto de acceso: credenciales y rol.
- Un titular **puede o no** tener usuario asociado. Una reserva creada por recepción o por un canal externo tiene titular sin cuenta.
- El dominio **no depende** del concepto de usuario. La autenticación pertenece a la infraestructura.

---

# 4. Lenguaje ubicuo

Vocabulario **obligatorio** en el modelo, el código, la API, la documentación y la sustentación. No se usan términos genéricos como *room*, *booking*, *user* o *request* cuando existe un término del negocio.

| Término | Significado |
|---|---|
| **Alojamiento** | El negocio completo: conjunto de apartamentos bajo una misma administración. |
| **Apartamento** | Unidad vendible. Vivienda autónoma e identificada, entregada en exclusiva a un grupo. |
| **Dormitorio** | Espacio para dormir dentro de un apartamento. Atributo descriptivo, nunca reservable por separado. |
| **Capacidad** | Número máximo de personas que admite un apartamento. Tope rígido. |
| **Noche** | Unidad mínima de venta, según la definición 3.1. |
| **Estancia** | Rango continuo de noches en que un apartamento queda ocupado por una reserva. |
| **Reserva** | Compromiso de ocupar un apartamento durante una estancia, para un conjunto definido de ocupantes. |
| **Titular** | Responsable de la reserva y de su pago. |
| **Ocupante** | Persona incluida en la reserva, con su fecha de nacimiento. |
| **Ocupante facturable** | Ocupante que a la fecha de entrada alcanza el umbral de edad definido. Solo estos generan cargo. |
| **Tarifa** | Valor por ocupante facturable, por noche, para un apartamento en una temporada. |
| **Temporada** | Periodo del calendario con tarifas propias. |
| **Temporada base** | Temporada que cubre todas las fechas no asignadas a otra temporada. Obligatoria. |
| **Disponibilidad** | Resultado del cálculo definido en 3.3. |
| **Bloqueo** | Registro administrativo que impide vender un apartamento en un rango de fechas. |
| **Estado operativo** | Condición física presente del apartamento. |
| **Tiempo de preparación** | Horas requeridas entre una salida y la siguiente entrada. |
| **Registro** | Momento en que el grupo toma posesión del apartamento (*check-in*). |
| **Salida** | Momento en que el apartamento se libera y queda pendiente de preparación (*check-out*). |
| **No-show** | El titular no se presenta antes de la hora límite del día de entrada. |
| **Política de cancelación** | Regla única del alojamiento que define retenciones y devoluciones según la antelación. Versionada. |
| **Folio** | Cuenta de la reserva: acumula cargos y pagos y determina el saldo. |
| **Cargo** | Concepto que suma al folio: alojamiento, servicio, penalidad, ajuste. |
| **Pago** | Abono registrado contra un folio, con medio y fecha. |
| **Saldo** | Diferencia entre cargos y pagos del folio. |
| **Canal** | Origen de la reserva: portal, directo o externo. |
| **Conflicto de canal** | Reserva externa rechazada por colisionar con una reserva vigente. |
| **Novedad** | Reporte de un daño, faltante o situación en un apartamento. |

Cada equipo puede **agregar** términos propios de su variante. No puede **renombrar ni eliminar** los de esta tabla.

---

# 5. Núcleo común — no negociable

Todos los equipos comparten el mismo esqueleto de negocio, porque las guías, los ejemplos de clase y las asesorías se apoyan en él.

| # | Elemento fijo |
|---|---|
| F-01 | La unidad vendible es el apartamento completo. Nunca habitaciones ni camas sueltas. |
| F-02 | El cobro es por persona, por noche, con un umbral de edad facturable. |
| F-03 | La capacidad del apartamento es un tope rígido, sin excepciones. |
| F-04 | Un apartamento no puede tener dos reservas activas solapadas, sin importar el canal. |
| F-05 | Existen temporadas que modifican la tarifa, incluida una temporada base obligatoria. |
| F-06 | La reserva tiene el ciclo de vida definido en la sección 8. |
| F-07 | Existe una única política de cancelación por alojamiento, versionada en el tiempo. |
| F-08 | Existe el folio, con cargos, pagos y saldo. Todo pago tiene medio y fecha. |
| F-09 | El apartamento tiene un estado operativo que condiciona su entrega. |
| F-10 | El alojamiento vende por los tres canales definidos en 2.4 sobre el mismo inventario. |
| F-11 | El lenguaje ubicuo de la sección 4 es obligatorio en código, documentación y sustentación. |
| F-12 | Las definiciones operativas de la sección 3 son obligatorias. |
| F-13 | El sistema administra **un único alojamiento**. |
| F-14 | La arquitectura, el stack y las prácticas de la sección 12. |

---

# 6. Decisiones libres — las define cada equipo

Los valores y variantes del negocio los define cada equipo, **se registran en la Ficha del Alojamiento (Anexo A)** y se sustentan en la primera entrega. No se aceptan valores arbitrarios: debe haber una razón de negocio detrás, aunque el negocio sea ficticio.

| # | Decisión | Mínimo exigido |
|---|---|---|
| L-01 | Nombre e identidad del alojamiento | Distinto al de otros equipos |
| L-02 | Ciudad y ubicación | Libre |
| L-03 | Cantidad de apartamentos | Entre 5 y 15 |
| L-04 | Identificación de cada apartamento | Única y estable |
| L-05 | Dormitorios y capacidad de cada apartamento | Al menos dos capacidades distintas |
| L-06 | Dotación y características | Al menos una característica diferenciadora |
| L-07 | Valores de las tarifas | Coherentes con el mercado elegido |
| L-08 | Temporadas: cuántas, nombres y fechas | Mínimo 2, además de la temporada base |
| L-09 | Umbral de edad facturable | Debe existir; el valor lo elige el equipo |
| L-10 | Plazos y retenciones de la política de cancelación | Mínimo 2 tramos de antelación |
| L-11 | Exigencia y monto del anticipo | Explícito, incluso si es "no se exige" |
| L-12 | Horas de entrada y salida | Deben existir horas definidas |
| L-13 | Tiempo de preparación entre estancias | Valor en horas |
| L-14 | Plazo de confirmación de una reserva pendiente | Valor en horas |
| L-15 | Hora límite para declarar no-show | Hora del día de entrada |
| L-16 | Mascotas: si se aceptan, con qué límites y si generan cargo | Decisión explícita |
| L-17 | Servicios adicionales y su cargo | Mínimo 1 |
| L-18 | Medios de pago aceptados | Mínimo 2 |
| L-19 | Reportes e indicadores adicionales | Sobre los mínimos obligatorios |
| L-20 | Tres reglas de negocio propias | Mínimo 3, aprobadas por el docente |
| L-21 | Caso de uso del modelo de IA | No bloqueante |
| L-22 | Integración externa | Mínimo 1 |
| L-23 | Funcionalidad opcional elegida | Mínimo 1 de la sección 11 |

> **Criterio de diseño derivado:** si algo cambia de un alojamiento a otro, **no puede estar quemado en el código**. El umbral de edad, las temporadas, los plazos y el tiempo de preparación son configuración, no constantes. Si el equipo tuvo que recompilar para cambiar un valor, el diseño está mal. Este punto se evalúa.

---

# 7. Funcionalidades esenciales (obligatorias)

## 7.1 Roles y acciones

### Huésped
- Registrarse e iniciar sesión.
- Buscar apartamentos disponibles por fechas, número de personas y características.
- Ver el detalle completo de un apartamento: galería, descripción, dotación, calendario de disponibilidad y ubicación en mapa.
- Cotizar una estancia antes de reservar, con el desglose noche por noche.
- Crear una reserva por el canal **portal**, indicando fechas, ocupantes con su fecha de nacimiento y hora estimada de llegada.
- Cancelar su propia reserva, sujeto a la política congelada en ella.
- Consultar su historial de reservas y el estado de cada una.
- Consultar el folio y el saldo **de sus propias reservas**.

### Recepcionista
- Iniciar sesión con su rol.
- Consultar disponibilidad y crear reservas por el canal **directo**, a nombre de un titular que puede no tener cuenta.
- Modificar fechas, composición del grupo y apartamento asignado de una reserva no iniciada.
- Confirmar, cancelar y marcar como no-show las reservas.
- Registrar la llegada y la salida de los grupos.
- Consultar las llegadas y salidas previstas del día.
- Registrar cargos y pagos en el folio, y cerrarlo.
- Consultar el calendario de ocupación.

### Administrador
- Todas las acciones del recepcionista.
- Gestionar los datos del alojamiento, sus horarios, normas y servicios.
- Gestionar apartamentos (CRUD con eliminación lógica) y bloqueos.
- Gestionar temporadas y tarifas.
- Configurar los parámetros de la sección 6.
- Definir y actualizar la política de cancelación.
- Gestionar los canales de venta y resolver los conflictos registrados.
- Gestionar usuarios y roles.
- Autorizar el cierre de un folio con saldo pendiente.
- Consultar todos los reportes.

### Personal de servicio
- Iniciar sesión con su rol.
- Consultar los apartamentos pendientes de preparación.
- Cambiar el estado operativo de un apartamento entre `PENDIENTE_PREPARACION`, `EN_PREPARACION` y `PREPARADO`.
- Registrar novedades sobre un apartamento.
- **No puede** declarar `FUERA_DE_SERVICIO` ni registrar bloqueos: eso es decisión administrativa.

## 7.2 Gestión de usuarios, roles y seguridad

**Registro**
- Campos obligatorios: nombre, correo electrónico (único), contraseña encriptada, teléfono, documento de identidad y fecha de nacimiento.
- Validaciones: formato de correo válido y contraseña segura (mínimo 8 caracteres, con mayúsculas y números).
- El registro público crea usuarios con rol **Huésped**. Los roles internos solo los asigna un Administrador.

**Autenticación y autorización**
- Autenticación mediante JWT.
- Cada endpoint se protege según el rol.
- Un huésped **nunca** puede acceder a reservas, folios ni datos de otros huéspedes, ni a la configuración del alojamiento. Esta restricción se verifica sobre el **propietario del recurso**, no solo sobre el rol.

**Perfil y contraseña**
- Actualizar nombre, teléfono y foto de perfil. El correo no es editable.
- Cambio voluntario de contraseña: con la contraseña actual y la nueva.
- Recuperación: código enviado al correo registrado, vigencia de 15 minutos, un solo uso.

**Eliminación**
- Las eliminaciones de usuarios son **lógicas**.
- No puede eliminarse un usuario que sea titular de reservas en estado `PENDIENTE`, `CONFIRMADA` o `EN_CURSO`.
- Eliminar el usuario **no elimina** al titular ni sus reservas históricas.

## 7.3 Gestión del alojamiento y sus apartamentos

**Datos del alojamiento**
- Nombre, descripción, ciudad, dirección y ubicación exacta (latitud y longitud).
- Hora de entrada y hora de salida.
- Normas de convivencia.
- Servicios adicionales, indicando si generan cargo y su valor.
- Parámetros configurables: umbral de edad facturable, tiempo de preparación, plazo de confirmación, hora límite de no-show, exigencia y monto del anticipo.

**Apartamentos**
- Atributos: identificación única, nombre, descripción, número de dormitorios, capacidad máxima, dotación y características.
- Imágenes: mínimo 1, máximo 10, con imagen principal destacada, almacenadas en un servicio externo.
- Un apartamento puede retirarse de la venta solo si no tiene reservas activas ni futuras. La eliminación es **lógica**.
- Los listados y búsquedas ignoran los apartamentos eliminados. Las reservas históricas los siguen referenciando.
- **Cambiar la capacidad de un apartamento no afecta las reservas ya creadas**, aunque las deje por encima de la nueva capacidad. El sistema advierte al administrador y las lista.

**Bloqueos**
- Registrar un bloqueo por rango de fechas, con motivo.
- No puede registrarse un bloqueo sobre noches que ya tengan reservas activas.
- Un apartamento bloqueado no aparece como disponible.

## 7.4 Temporadas, tarifas y políticas

**Temporadas**
- Definir temporadas con nombre y rangos de fechas.
- Las temporadas **no pueden solaparse entre sí**.
- Existe una **temporada base obligatoria** que cubre todas las fechas no asignadas. Ninguna fecha reservable queda sin temporada.

**Tarifas**
- Definir el valor **por ocupante facturable, por noche**, para cada apartamento en cada temporada.
- **Todo apartamento activo debe tener tarifa en todas las temporadas.** El sistema impide activar un apartamento sin tarifas completas.
- Consultar el histórico de tarifas.

**Cotización**
- Calcular el valor de una estancia antes de reservar.
- Mostrar el **desglose noche por noche**: fecha, temporada, tarifa aplicada, ocupantes facturables y subtotal.

**Política de cancelación**
- Una sola política vigente, con al menos dos tramos de antelación y su retención.
- Al actualizarla **se conserva la versión anterior**. Cada reserva queda ligada a la versión vigente **al momento de su creación**.
- La política define qué ocurre ante cancelación y ante no-show.

## 7.5 Disponibilidad y reservas

**Búsqueda**
- Consultar apartamentos disponibles indicando rango de fechas y composición del grupo (cantidad de personas y fechas de nacimiento).
- Filtros: capacidad, rango de precio, características y servicios.
- Resultados en tarjetas con imagen principal, capacidad, valor estimado y ubicación.
- Mapa con la ubicación del alojamiento.
- **Paginación de 10 resultados por página** en todos los listados.

**Creación de reserva**
- Datos: titular, ocupantes con fecha de nacimiento, apartamento, fecha de entrada, fecha de salida, hora estimada de llegada y canal de origen.
- El sistema valida, en este orden:
  1. Fecha de salida posterior a la de entrada.
  2. Fecha de entrada no anterior a hoy.
  3. Apartamento activo y con tarifas completas.
  4. Capacidad suficiente para el total de ocupantes.
  5. Ausencia de solapamiento con reservas activas y con bloqueos.
  6. Tiempo de preparación respecto de la estancia anterior.
- Al crear la reserva se congela su valor y su política, se abre el folio y se notifica al titular.
- **La reserva nace en estado `PENDIENTE`.**

**Modificación**
- Cambiar fechas, ocupantes o apartamento asignado mientras la reserva esté en `PENDIENTE` o `CONFIRMADA` y no haya iniciado.
- Toda modificación repite las seis validaciones anteriores y recalcula el valor con las tarifas vigentes.
- La diferencia se registra como **ajuste** en el folio, positivo o negativo. **No se recalcula la política congelada.**

**Cancelación**
- Puede cancelarse desde `PENDIENTE` o `CONFIRMADA`, siempre antes del registro.
- El sistema calcula retención y devolución con la **política congelada en la reserva** y las registra como cargo y como saldo a favor en el folio.
- La cancelación libera la disponibilidad de inmediato.
- **La salida anticipada de un grupo ya registrado no es una cancelación**: se maneja como salida, y la política no aplica.

**No-show**
- Puede declararse a partir de la hora límite configurada del día de entrada.
- Aplica la consecuencia definida en la política y libera la disponibilidad.

**Vencimiento de reservas pendientes**
- Una reserva `PENDIENTE` que supere el plazo de confirmación configurado pasa a `CANCELADA` y libera la disponibilidad.
- El vencimiento debe ocurrir **sin intervención manual**.

**Listados**
- Reservas ordenadas de la más reciente a la más antigua.
- Filtros por fechas, estado, apartamento, canal y titular.
- Calendario de ocupación por apartamento y rango de fechas.

## 7.6 Operación diaria

**Registro y salida**
- Registrar la llegada validando: reserva en `CONFIRMADA`, fecha de entrada alcanzada, y apartamento en estado `PREPARADO`.
- Registrar la salida: la reserva pasa a `FINALIZADA` y el apartamento a `PENDIENTE_PREPARACION`.
- El cierre del folio es requisito para completar la salida, salvo autorización del administrador.
- Consultar las llegadas y salidas previstas para un día.

**Estado operativo**
- Cada apartamento tiene en todo momento un estado, según 3.3.
- Transiciones permitidas: `PREPARADO → OCUPADO` (registro), `OCUPADO → PENDIENTE_PREPARACION` (salida), `PENDIENTE_PREPARACION → EN_PREPARACION → PREPARADO` (servicio), y `FUERA_DE_SERVICIO` desde cualquier estado no ocupado, solo por administrador.

**Novedades**
- Registrar novedades sobre un apartamento con fecha, autor, descripción y gravedad.
- Consultar el historial de novedades de un apartamento.

## 7.7 Folio, cargos y pagos

- **El folio se abre al crear la reserva**, con el cargo de alojamiento calculado. Esto permite registrar el anticipo antes de la llegada.
- Un folio pertenece a **una sola reserva**.
- Tipos de cargo: alojamiento, servicio adicional, ajuste por modificación, penalidad por cancelación o no-show.
- Registrar pagos parciales indicando **medio de pago y fecha**. Un mismo folio admite medios distintos.
- El saldo es la diferencia entre la suma de cargos y la suma de pagos. Puede ser positivo (debe el huésped), cero o negativo (saldo a favor).
- Consultar el folio con su detalle completo.
- El folio no puede cerrarse con saldo distinto de cero sin **autorización explícita del administrador**, que queda registrada con autor y motivo.
- **Los cargos y pagos no se editan ni se borran.** Una corrección se hace con un movimiento inverso.

## 7.8 Canales externos e integraciones

- Registrar los canales de venta, con su tipo y su credencial de acceso.
- Exponer a un canal externo, mediante el contrato de la sección 10.3:
  - Consulta de disponibilidad por rango de fechas.
  - Creación de reserva externa.
  - Cancelación de reserva externa.
- Toda reserva externa llega con un **identificador propio del canal**. La combinación canal + identificador externo es **única**: recibir dos veces el mismo mensaje **no crea dos reservas**.
- Una reserva externa que colisione con una reserva vigente **se rechaza y se registra como conflicto** para revisión del administrador. **Nunca sobrescribe la reserva existente.**
- Consultar la bitácora de eventos intercambiados con los canales.
- Notificar al titular la confirmación y la cancelación de su reserva.

## 7.9 Reportes

Con filtro por rango de fechas en todos los casos:

- **Ocupación:** noches vendidas, y porcentaje de ocupación calculado como noches vendidas dividido entre (apartamentos activos × noches del periodo).
- **Ingresos:** suma de **pagos registrados** en el periodo, discriminada por medio de pago. Los cargos no pagados no cuentan como ingreso.
- **Reservas por canal de origen**, incluyendo canceladas y no-show.

---

# 8. Ciclo de vida de la reserva

Los estados y las transiciones son **los mismos para todos los equipos**.

| Estado | Significado | ¿Retiene disponibilidad? |
|---|---|---|
| `PENDIENTE` | Creada, aún no confirmada. Vence al cumplirse el plazo de confirmación. | Sí |
| `CONFIRMADA` | En firme. | Sí |
| `EN_CURSO` | El grupo realizó el registro y ocupa el apartamento. | Sí |
| `FINALIZADA` | El grupo salió. | No |
| `CANCELADA` | Terminada antes del registro. | No |
| `NO_SHOW` | El titular no se presentó antes de la hora límite. | No |

**Transiciones válidas.** Toda transición no listada debe ser rechazada por el dominio.

| Desde | Hacia | Disparador |
|---|---|---|
| `PENDIENTE` | `CONFIRMADA` | Confirmación, con anticipo si el equipo lo exige |
| `PENDIENTE` | `CANCELADA` | Cancelación o vencimiento del plazo de confirmación |
| `CONFIRMADA` | `EN_CURSO` | Registro del grupo |
| `CONFIRMADA` | `CANCELADA` | Cancelación |
| `CONFIRMADA` | `NO_SHOW` | Vencida la hora límite del día de entrada |
| `EN_CURSO` | `FINALIZADA` | Salida del grupo |

`FINALIZADA`, `CANCELADA` y `NO_SHOW` son estados **terminales**. Se consideran **reservas activas** las que están en `PENDIENTE`, `CONFIRMADA` o `EN_CURSO`.

---

# 9. Reglas de negocio invariantes

Obligatorias para todos los equipos. **Todas deben tener prueba unitaria, incluyendo el caso en que se violan.**

| ID | Regla |
|---|---|
| RN-01 | Un apartamento no puede tener dos reservas **activas** solapadas, según la definición de solapamiento de 3.1 y sin importar el canal de origen. |
| RN-02 | El número total de ocupantes no puede exceder la capacidad del apartamento. Sin excepciones. |
| RN-03 | La fecha de salida es posterior a la de entrada: toda estancia tiene al menos una noche. |
| RN-04 | No se pueden crear reservas cuya fecha de entrada sea anterior a la fecha actual. |
| RN-05 | El valor de la estancia es la suma, noche por noche, de la tarifa vigente del apartamento en la temporada de esa noche, multiplicada por el número de ocupantes facturables. |
| RN-06 | Un ocupante es facturable si a la fecha de entrada alcanza el umbral configurado. Los no facturables sí cuentan para la capacidad. |
| RN-07 | Un apartamento con bloqueo vigente sobre una noche no está disponible para esa noche. |
| RN-08 | La reserva solo transita entre los estados permitidos en la sección 8. Toda transición inválida se rechaza. |
| RN-09 | Toda reserva debe registrar hora estimada de llegada antes de confirmarse. |
| RN-10 | No se permite el registro antes de la fecha de entrada, ni sobre una reserva que no esté `CONFIRMADA`. |
| RN-11 | Un apartamento solo puede recibir un grupo si su estado operativo es `PREPARADO`. |
| RN-12 | Toda reserva que deja de estar activa libera sus noches de forma inmediata. |
| RN-13 | La retención por cancelación y por no-show se determina por la **versión de política congelada en la reserva**, no por la vigente al momento del hecho. |
| RN-14 | Todo cambio de fechas, ocupantes o apartamento revalida las seis condiciones de creación y recalcula el valor, registrando la diferencia como ajuste. |
| RN-15 | Todo pago se registra contra un folio con su medio y su fecha. El saldo es la diferencia entre cargos y pagos. |
| RN-16 | Los cargos y pagos no se modifican ni se eliminan: toda corrección es un movimiento inverso. |
| RN-17 | El folio no puede cerrarse con saldo distinto de cero sin autorización explícita registrada. |
| RN-18 | Una reserva externa en conflicto se rechaza y se registra; nunca sobrescribe la reserva vigente. |
| RN-19 | La combinación canal más identificador externo es única: un mismo mensaje recibido dos veces no crea dos reservas. |
| RN-20 | Entre la salida de un grupo y la entrada del siguiente debe respetarse el tiempo de preparación configurado, según 3.3. |
| RN-21 | Una reserva `PENDIENTE` que supera el plazo de confirmación se cancela automáticamente. |
| RN-22 | El valor y la política de una reserva quedan congelados al crearla y solo cambian por modificación explícita. |

---

# 10. Elementos de diferenciación (obligatorios, de contenido libre)

Los tres son de implementación obligatoria; su contenido lo decide el equipo y se sustenta ante el docente.

## 10.1 Reglas de negocio propias

- Mínimo **tres reglas adicionales**, coherentes con el alojamiento definido y **aprobadas por el docente antes de implementarlas**.
- Deben ser **verificables y con consecuencia**: algo que el sistema pueda rechazar o calcular distinto. "Ofrecer buen servicio" no es una regla.
- Deben tener prueba unitaria, igual que las invariantes.
- Ejemplos de partida: estancia mínima en temporada alta, descuento por estancias largas, recargo por llegada nocturna, cupo de vehículos en el parqueadero, límite de mascotas por apartamento, depósito reembolsable, tarifa diferencial por canal.

## 10.2 Modelo de inteligencia artificial

- El equipo incorpora un modelo de IA en el punto donde vea mayor valor.
- **Restricción obligatoria:** el componente es **no bloqueante**. Si el modelo falla, tarda o no está configurado, el sistema opera con normalidad y la funcionalidad afectada se degrada de forma visible pero no impide operar. La IA **nunca** es requisito para reservar, cobrar o registrar a un huésped.
- **Verificación:** en la sustentación se debe demostrar el sistema funcionando **con la IA deshabilitada**. Debe bastar un cambio de configuración, sin tocar código.
- **Viabilidad:** se acepta cualquier proveedor con nivel gratuito o de prueba, y también un modelo ejecutado localmente. La clave de acceso **nunca** se versiona.
- Ejemplos de partida: sugerencia de tarifa según ocupación y temporada, resumen diario de llegadas y salidas, clasificación de novedades por gravedad, redacción de la confirmación al huésped, detección de reservas atípicas.

## 10.3 Integraciones con servicios externos

Esta sección define **qué debe funcionar de verdad** y **con qué se puede lograr**. Ninguna integración puede depender de un contrato comercial, una afiliación empresarial ni un pago real.

### 10.3.1 Canal externo (obligatorio para todos)

El canal externo **no es una plataforma comercial real**. Es un contrato definido por el curso, que el equipo implementa de ambos lados:

- **Lado servidor:** el SGA expone tres operaciones autenticadas con una credencial por canal:
  1. Consultar disponibilidad para un rango de fechas y una composición de grupo.
  2. Crear una reserva externa, identificada por el canal y su identificador propio.
  3. Cancelar una reserva externa previamente creada.
- **Lado cliente:** el equipo entrega un **simulador de canal** que consuma esas operaciones. Es válido cualquiera de estos formatos, y todos son realizables sin costo:
  - Una colección de peticiones (Postman, Bruno, Insomnia o archivo `.http`) con los escenarios de prueba.
  - Un script o una aplicación mínima independiente que envíe las peticiones.
- **Escenarios que deben demostrarse:** reserva externa aceptada, reserva externa en conflicto rechazada y registrada, y **mensaje repetido que no duplica la reserva**.

### 10.3.2 Integración de salida (mínimo una)

El equipo elige **al menos una** y la implementa como **adaptador de salida** sustituible:

**Opción A — Notificaciones.** Correo electrónico o mensajería instantánea. Alternativas realizables sin costo: un servidor SMTP con clave de aplicación, un servicio de correo transaccional con nivel gratuito, un servicio de captura de correo para pruebas, o la API de un bot de mensajería.

**Opción B — Pasarela de pagos en modo de prueba.** Únicamente en entorno de pruebas o *sandbox*, con tarjetas de prueba. **Está prohibido mover dinero real.** El resultado del pago se registra como un pago en el folio; el sistema no ejecuta desembolsos.

### 10.3.3 Requisitos comunes a toda integración externa

Aplican al canal externo, a la integración de salida, al almacenamiento de imágenes, al servicio de mapas y al modelo de IA:

| # | Requisito |
|---|---|
| I-01 | Debe implementarse detrás de un **puerto del dominio**. El dominio no conoce al proveedor. |
| I-02 | Debe existir una **implementación alternativa local** (simulada o en memoria) que permita ejecutar y probar el sistema **sin conexión a internet y sin credenciales**. |
| I-03 | Debe definirse un **tiempo de espera máximo** explícito. |
| I-04 | Debe definirse el **comportamiento ante fallo**: qué ve el usuario, qué se registra y si la operación de negocio continúa o se detiene. |
| I-05 | El fallo de una integración **no puede impedir** reservar, registrar, cobrar ni cerrar un folio. |
| I-06 | Las credenciales se manejan por **configuración externa** y no se versionan. |
| I-07 | Debe seleccionarse una alternativa con **nivel gratuito, de prueba o local**. Si exige tarjeta de crédito o contrato, no es admisible para el curso. |
| I-08 | Las pruebas automáticas **no invocan servicios externos reales**: usan la implementación alternativa de I-02. |

> El punto I-02 es el que hace realizable todo lo demás: el proyecto debe poder evaluarse en un salón sin internet, con las integraciones apagadas, y aun así funcionar.

---

# 11. Funcionalidades opcionales

Cada equipo implementa **al menos una** (L-23). Las demás suman en la valoración.

1. **Lista de espera.** Cuando no hay disponibilidad, el interesado se anota y el sistema le avisa si se libera el rango por una cancelación.
2. **Sobreventa controlada.** Margen de sobreventa configurable, con reubicación asistida cuando el conflicto se materializa.
3. **Cupones y promociones.** Códigos de descuento aplicables al cotizar, con vigencia y condiciones.
4. **Programa de huéspedes frecuentes.** Beneficios o tarifas diferenciales para titulares recurrentes.
5. **Chat huésped–recepción.** Comunicación en tiempo real tras confirmar la reserva.
6. **Recordatorios automáticos.** Avisos programados antes de la llegada y antes de la salida.
7. **Gestión de insumos.** Control de consumibles asociados a la preparación de apartamentos.
8. **Exportación de reportes.** Descarga en hoja de cálculo o PDF.

---

# 12. Restricciones técnicas y prácticas

## 12.1 Arquitectura

- El sistema se construye con **arquitectura hexagonal** (puertos y adaptadores).
- El **dominio no depende del framework**: no conoce Spring, JPA, HTTP ni la base de datos.
- Toda regla de negocio se valida **en el dominio**. La validación en la interfaz es adicional, nunca sustituta.
- Ninguna regla puede vivir únicamente en un controlador, un repositorio o un componente del frontend.

## 12.2 Tecnología

- Backend: **Java con Spring Boot** y **Gradle**.
- Frontend: **React**.
- Persistencia en desarrollo: **H2**, con datos de prueba cargados al iniciar.
- Persistencia en despliegue: se admite H2 en modo archivo o una base de datos gestionada. **El cambio no puede requerir modificar el dominio**; es una sustitución de adaptador y así debe demostrarse.
- Imágenes: servicio externo de almacenamiento con nivel gratuito.
- Mapas: se recomienda Mapbox.

## 12.3 Datos de prueba mínimos

Al iniciar, el sistema debe quedar con datos suficientes para operar y demostrar:

- Todos los apartamentos declarados en la Ficha, con sus tarifas completas en todas las temporadas.
- Al menos un usuario por rol.
- Reservas en **todos** los estados del ciclo de vida.
- Al menos un bloqueo vigente y un apartamento fuera de servicio.
- Al menos una reserva por cada uno de los tres canales.

## 12.4 Prácticas de desarrollo

- Repositorio en **GitHub**, con historial de commits progresivo. **Todos los integrantes deben contribuir de forma verificable.**
- **Ningún secreto** se versiona. Se manejan por configuración externa.
- Las eliminaciones de registros son **lógicas**, nunca físicas.
- Todos los listados de resultados usan **paginación de 10 por página**.
- La API expone códigos de estado HTTP adecuados y devuelve errores con **significado de negocio**, no trazas técnicas.
- Existen **pruebas unitarias** sobre todas las reglas invariantes de la sección 9 y sobre las reglas propias del equipo, incluidos sus casos de falla.
- Los valores de la sección 6 son **configuración**, no constantes en el código.

---

# 13. Entregas

Criterio rector: **cada corte entrega software que funciona**, no una capa suelta.

| Corte | Alcance |
|---|---|
| **1** | Ficha del Alojamiento (Anexo A), modelo del negocio, casos de uso, glosario propio, y el **dominio implementado con sus reglas invariantes y sus pruebas unitarias** |
| **2** | Usuarios y seguridad. Alojamiento, apartamentos, bloqueos, temporadas y tarifas. Disponibilidad, cotización y reservas **operando por API** |
| **3** | Interfaz de usuario completa: búsqueda, detalle, reserva, registro y salida, estado operativo, folio y cancelaciones |
| **4** | Canal externo con manejo de conflictos e idempotencia, integración de salida, modelo de IA no bloqueante, reportes, funcionalidad opcional y **sistema desplegado** |

En cada corte se evalúa, además del producto: coherencia con el lenguaje ubicuo, ubicación correcta de la lógica de negocio, calidad del historial de commits y capacidad de sustentar las decisiones tomadas.

---

# Anexo A. Ficha del Alojamiento

Entregable obligatorio del **Corte 1**. Todo lo aquí definido debe ser **configuración del sistema**, nunca valores quemados en el código.

## A.1 Identidad

| Campo | Valor |
|---|---|
| Nombre del alojamiento | |
| Ciudad y ubicación | |
| Descripción breve | |
| Integrantes del equipo | |

## A.2 Inventario

| ID | Nombre | Dormitorios | Capacidad | Características |
|---|---|---|---|---|
| | | | | |
| | | | | |
| | | | | |
| | | | | |
| | | | | |

*Mínimo 5 apartamentos, con al menos dos capacidades distintas.*

## A.3 Temporadas y tarifas

| Temporada | Fechas que cubre | Criterio de negocio |
|---|---|---|
| Base | Resto del año | |
| | | |
| | | |

| Apartamento | Temporada | Tarifa por ocupante facturable por noche |
|---|---|---|
| | | |

*Toda combinación apartamento × temporada debe tener tarifa.*

## A.4 Parámetros y políticas

| Parámetro | Valor | Justificación |
|---|---|---|
| Umbral de edad facturable | | |
| Hora de entrada | | |
| Hora de salida | | |
| Tiempo de preparación (horas) | | |
| Plazo de confirmación (horas) | | |
| Hora límite para no-show | | |
| ¿Se exige anticipo? ¿De cuánto? | | |
| ¿Se aceptan mascotas? Límites y cargo | | |
| Normas de convivencia | | |

**Política de cancelación** (mínimo dos tramos):

| Antelación | Retención | Devolución |
|---|---|---|
| | | |
| | | |

| Consecuencia del no-show | |
|---|---|

## A.5 Canales, pagos y servicios

| Campo | Valor |
|---|---|
| Canales de venta habilitados | |
| Medios de pago aceptados | |
| Servicios adicionales y su cargo | |

## A.6 Diferenciación del equipo

| Elemento | Definición |
|---|---|
| Regla propia 1 | |
| Regla propia 2 | |
| Regla propia 3 | |
| Caso de uso del modelo de IA | |
| Comportamiento con la IA deshabilitada | |
| Integración de salida elegida y proveedor | |
| Comportamiento del sistema si la integración falla | |
| Funcionalidad opcional elegida | |

---

# Anexo B. Preguntas guía para definir el alojamiento

Sirven para construir una Ficha coherente y no una lista de valores arbitrarios. Son, además, **las mismas preguntas que se le harían al dueño de un alojamiento real**: el ejercicio equivale a una sesión de levantamiento de requisitos.

**Inventario.** ¿Cuántos apartamentos hay y cómo los identifica el negocio? ¿Qué diferencia a uno de otro? ¿Por qué alguien pagaría más por uno que por otro? ¿Alguno tiene limitaciones de acceso que deban advertirse al reservar?

**Reserva.** ¿En qué momento una consulta se vuelve una reserva en firme? ¿Cuánto tiempo se espera a que alguien confirme antes de liberar las fechas? ¿Se pueden cambiar fechas después de confirmar? ¿Qué se hace cuando llegan más personas de las anunciadas?

**Llegada y salida.** ¿Cuáles son las horas de entrada y salida? ¿Qué pasa si el huésped quiere llegar antes o irse después? ¿Hasta qué hora se espera a alguien antes de dar la reserva por perdida? ¿Qué se revisa antes de dar por cerrada una estancia?

**Dinero.** ¿Cómo se calcula el precio de una estancia? ¿Desde qué edad se cobra como adulto? ¿Cuáles son las temporadas y por qué esas fechas? Si una estancia cruza dos temporadas, ¿cómo se cobra? ¿En qué momento se cobra: al reservar, al llegar, al salir?

**Cancelaciones.** ¿Con cuánta antelación se puede cancelar y qué se devuelve en cada caso? ¿Qué pasa con las reservas hechas bajo una política anterior si la política cambia?

**Operación.** ¿Cuánto tiempo necesita un apartamento entre una salida y la siguiente llegada? ¿Cómo se sabe que ya está listo para entregar? ¿Qué se hace si no está listo cuando llega el huésped? ¿Quién reporta los daños?

**Canales.** ¿En qué canales se vende? ¿Cómo se evita vender dos veces la misma fecha? ¿Qué se hace cuando el conflicto ya ocurrió?

> **Método:** ante cada regla que se defina, preguntarse siempre **"¿y qué pasa cuando no se cumple?"**. Las excepciones son donde vive el negocio real, y son las que convierten un CRUD en un sistema.

---

**Universidad del Quindío 💚 2026**