package de.tub.mcc.fogmock.nodemanager.graphserv;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;


/*
 * This class handles malformed JSON bodies 
 */
@Provider
public class ExceptionMapperUnrecognizedProperty implements ExceptionMapper<UnrecognizedPropertyException> {
    @Override
    public Response toResponse(UnrecognizedPropertyException exception) {
        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity( exception.getMessage() )
                .type( MediaType.TEXT_PLAIN)
                .build();
    }
    
}
