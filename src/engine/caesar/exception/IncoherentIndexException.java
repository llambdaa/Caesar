package engine.caesar.exception;

public class IncoherentIndexException extends Exception {

    public IncoherentIndexException( int max, int missing ) {

        super( String.format( "Incoherent index '%s' found, but index '%s' is missing.\nConsider closing the index gap by defining fields for consecutive indices.", max, missing ) );

    }

}
