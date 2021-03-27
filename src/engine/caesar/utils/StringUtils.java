package engine.caesar.utils;

public class StringUtils {

    public static String[] split( String target, String firstOccurrence ) {

        return StringUtils.split( target, target.indexOf( firstOccurrence ) );

    }

    public static String[] split( String target, int index ) {

        String[] result = new String[ 2 ];
        result[ 0 ] = target.substring( 0, index );
        result[ 1 ] = target.substring( index + 1 );

        return result;

    }

}
