package ar.com.siripo.arcache;

/**
 * Interfaz completa, reune tanto mecanismos de acceso, mecanismos de
 * invalidacion y mecanismos de configuracion
 * 
 * @author Mariano Santamarina
 *
 */
public interface ArcacheClientInterface
		extends ArcacheConfigurationInterface, ArcacheReadWriteInterface, ArcacheInvalidationInterface {

}
