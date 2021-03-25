package engine.caesar.exception;

public class IncoherentFieldsException extends Exception {

    public IncoherentFieldsException( int max, int missing ) {

        super( String.format( "Field incoherence detected - argument at index '%s' found but index '%s' is not claimed.", max, missing ) );

    }

}
