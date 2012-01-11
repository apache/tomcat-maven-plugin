#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.tomcat.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * HelloService!
 */
@Path( "HelloService" )
public interface HelloService
{
    @Path( "sayHello/{who}" )
    @GET
    @Produces( { MediaType.TEXT_PLAIN } )
    String sayHello( @PathParam( "who" ) String who );
}
