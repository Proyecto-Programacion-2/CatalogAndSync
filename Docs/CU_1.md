# Caso de Uso 1
## Cuenta del usuario final

En este documento se aclaran los requisitos que tendrá el usuario final (el que usa la aplicación de KMP). Este caso de uso atraviesa ambos backends del proyecto (CatalogAndSync y TurnosYReservas) y no debe leerse como responsabilidad exclusiva de un solo servicio.

### 1.1 Register
El usuario deberá poder crear una nueva cuenta, con login y contraseña.
El login deberá ser único.
El email no debe ser repetido ni obligatorio.
El usuario tiene que ingresar los siguientes datos al crear la cuenta: login, email, password, firstName, lastName, langKey e imageUrl.
imageUrl es opcional y contiene la URL de la imagen de perfil del usuario.
Login y password son obligatorios. La password deberá ser hasheada y salada en este servicio de backend y nunca almacenada en texto plano.
langKey es el lenguaje que el usuario usará en la app, el valor default: "es".

### 1.2 Login
El usuario debe ser capaz de iniciar sesión con su login y contraseña.
El usuario final NUNCA puede acceder a los servicios de la cátedra directamente. Los servicios backend del proyecto (CatalogAndSync y TurnosYReservas) actúan como intermediarios ante la cátedra.

### 1.3 Consulta Turnos
El usuario deberá poder ver los profesionales y turnos disponibles.
Las disponibilidades de turnos pueden estar o no actualizadas al día de la consulta.

### 1.4 Reserva de Turnos
El usuario deberá poder reservar turnos con el profesional y en el día indicado.
El flujo de reserva es el siguiente:
1. El usuario visualiza los turnos disponibles.
2. El usuario elige un turno.
3. El servicio consulta a la cátedra si el turno ya fue reservado por otra persona.
4. Si el turno está disponible, se crea el hold en la cátedra.
5. Si el turno no está disponible, se muestra una excepción al usuario, se actualiza el listado de turnos disponibles y se vuelve al punto 1.3 de este CU.
Una vez creado el hold, se lo confirma inicialmente mediante REST. La cátedra publica el evento AdditionalInformationRequested por Kafka y recién entonces se le pide información adicional al usuario (número de teléfono).
El hold tiene una vigencia limitada: la define la cátedra mediante expiresAt y una copia local no la extiende. Si el usuario no completa la información antes del vencimiento, el hold expira.

### 1.5 Listado de turnos
El usuario deberá poder ver el listado de sus turnos, así como fecha y profesional del turno.
El estado de los procesos y reservas, asociado a cada usuario final, es responsabilidad del servicio TurnosYReservas; CatalogAndSync no almacena turnos del usuario.
Además el usuario deberá ser notificado si un turno se cancela de manera externa.