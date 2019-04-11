package ar.com.siripo.arcache;

import java.io.Serializable;

/*
 * El valor a almacenar en el backend para permitir la expiracion de sets
 * 
 * Para todas las comparaciones de fechas se considerará la siguiente lógica. Se
 * cosidera la granularidad del tiempo en segundos, se considera ademas que
 * puede haber un error entre los relojes.
 * 
 * Por mas que se utilicen relojes sincronizados al yoctosegundo, al utilizar
 * segundo como unidad de medida Se debe considerar un error de al menos un
 * segundo.
 * 
 * LLamamos timeMeasurementError a la canitdad de segundos de error entre 2
 * relojes + el error propio de medir en segundos. Este valor jamas podra ser
 * cero, y se debe ajustar a las caracteristicas de la infraestuctura donde será
 * utilizado el sistema. Para ser mas riguroso, se debe añadir a este error el
 * tiempo que demanda construir una key.
 * 
 * Si por ejemplo para crear un valor se accede a una base de datos. El time de
 * este valor se deberia haber calculado antes de iniciar el acceso a la base de
 * datos, para tener de esa forma una marca clara de cual fue el instante de
 * validez de esa clave.
 * 
 * Entonces se puede calcular timeMeasurementError = 1 + maxClockOffset +
 * maxKeyCreationTime
 * 
 * maxClockOffset (offset maximo entre 2 relojes de cualquiera de las maquinas
 * intervinientes) maxKeyCreationTime (tiempo maximo de creacion de un valor a
 * ser almacenado)
 * 
 * Ahora para los calculos se puede considerar simplemente que las keys
 * almacenadas se hayan almacenado en el pasado. Este pasado sera calculado
 * restando el momento en que se almaceno un valor y el timeMeasurementError
 * 
 * Por ejempo si una key tiene como momento de almacenamiento readedStoreTime El
 * momento efectivo a ser utilizado para determinar la validez sera:
 * effectiveStoreTime = readedStoreTime - timeMeasurementError
 * 
 * Entonces una key será considerada invalida si. effectiveStoreTime <=
 * invalidationTimestamp
 * 
 * Es MUY importante el IGUAL en la comparacion para evitar ambiguedades. La
 * comparacion anterior tambien se puede leer como: Una clave es considerada
 * VALIDA si: effectiveStoreTime > invalidationTimestamp
 * 
 * 
 * Se puede ver fácilmente que si timeMeasurementError es un valor grande. Y
 * ademas invalidationWindow es cero o pequeña, generará muchisimos errores.
 * 
 * Imaginar la siguiente situacion: se marca como invalida una key en time
 * STORETIME al mismo instante, en la misma maquina, y asumiendo todo perfecto,
 * se invalida esa key con time exactamente igual a STORETIME. Lo que deberia
 * ocurrir es que esta clave sea actualizada, porque puede haber ocurrido que
 * haya sido guardada y luego invaldiada (en ese orden estricto). Pero solo se
 * quiere actualizar una vez. La comparacion ( effectiveStoreTime = STORETIME -
 * timeMeasurementError ) <= STORETIME (invalidationTimestamp) sera true
 * 
 * Para solucionar esto, una key sera considerada VALIDA (Y no se verificara
 * nada mas sobre su validez) cuando: NOW - timeMeasurementError <=
 * readedStoreTime
 * 
 * ej. timeMeasurementError=3 STORETIME=readedStoreTime=invalidationTimestamp=10
 * 
 * 10-3 <= 10 (true) 13-3 <= 10 (true) 14-3 <= 10 (false)
 * 
 * Lo anterior evita que una key se actualice alocadamente cuando
 * timeMeasurementError es un valor grande.
 * 
 * Se sugiere dado que existe esta proteccion que timeMeasurementError tenga un
 * valor aproximado de 4 Lo que sera 1 segundo de error base + 2 segundos de
 * offset entre relojes + 1 segundo de tiempo de generacion
 * 
 * @author Mariano Santamarina
 *
 */
public class CacheInvalidationObject implements CacheInvalidationObjectType, Serializable {

	private static final long serialVersionUID = 20180410001L;

	/**
	 * unix timestamp in milliseconds del momento en el cual se pide la invalidacion
	 */
	public long invalidationTimestampMillis;

	/**
	 * Indica si la invalidacion es hard o soft. En el caso de hard si se determina
	 * que se ha invalidado, el valor almacenado queda inaccesible. Esto permite
	 * destruir el acceso a valores que generarian inconsistencias intolerables
	 * 
	 * El caso soft, el valor sera recuperado pero marcando que ha sido invalidado.
	 * Lo cual permite seguir utilizando el valor si es imposible reconstruirlo por
	 * una degradacion fuerte del sistema
	 */
	public boolean isHardInvalidation;

	/**
	 * Cantidad de tiempo en segundos que dura la ventana de invalidacion. Setearla
	 * en cero fuerza una invalidacion instantanea. Si se setea por ejemplo un valor
	 * de 10 se darán 10 segundos con probabilidad creciente de que el valor sea
	 * considerado invalido. Esto permite que una invalidacion no colapse el
	 * sistema.
	 */
	public long invalidationWindowMillis;

	/**
	 * if this has a value greater than zero, all the keys older than this timestamp
	 * are considered hard invalidated.
	 */
	public long lastHardInvalidationTimestampMillis;

	/**
	 * if this has a value greater than zero, all the keys older than this timestamp
	 * are considered soft invalidated.
	 */
	public long lastSoftInvalidationTimestampMillis;

}
