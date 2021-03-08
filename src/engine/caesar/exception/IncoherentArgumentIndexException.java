package engine.caesar.exception;

public class IncoherentArgumentIndexException extends Exception {

    public IncoherentArgumentIndexException(int max, int missing ) {

        super( String.format( "Incoherent index '%s' found, but index '%s' is missing.\nConsider closing the index gap by defining fields for consecutive indices.", max, missing ) );

    }

}
