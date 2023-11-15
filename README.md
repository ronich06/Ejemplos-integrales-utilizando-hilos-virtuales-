# Ejemplos-integrales-utilizando-hilos-virtuales-
Autores:

Andrés León Orozco

Eduardo Ojeda Paladino

Rony Chinchilla Azofeifa

Kairo Chacon Maleaños

Este proyecto es meramente para uso académico.

Para la sección de ejemplos decidimos realizar un programa clásico de productor/consumidor a partir de uno hecho en el curso de Sistemas Operativos para el manejo de concurrencia, donde n hilos productores se encargan de ir leyendo byte por byte de un archivo y metiéndolos en un buffer según van siendo leídos, cuando este buffer se llena se para de leer bytes y se envía un aviso a los hilos consumidores de que pueden empezar a contar. Luego n hilos consumidores revisan cada elemento del buffer y suman 1 en un vector de 256 posiciones, donde la posición es el numero de byte y el contenido la cantidad de bytes, cada vez que cuentan liberan el espacio en el buffer, una vez el buffer queda vacío nuevamente se para de contar y se avisa a los hilos productores de que pueden seguir leyendo.

Cabe destacar que este programa tiene como objetivo contar byte por byte ya que en el problema original se requería comprimir el archivo, y el contar byte por byte es parte importante para ello. Por esta razón a mas hilo mas dura el programa realizándolo, debido a la sobrecarga de espera, ya que el objetivo del ejemplo era mostrar la diferencia en eficiencia de los hilos virtuales en contra de los a partir de la versión 19 de Java llamados "hilos de plataforma" decidimos dejarlo así. Cabe destacar que una optimización para este ejemplo seria separar el archivo en una cantidad de partes iguales al número de hilos productores para así realizar la lectura de bytes paralelamente y a más hilos mejor rendimiento(Hasta que la cantidad de bytes sea menor o igual a la cantidad de hilos).

