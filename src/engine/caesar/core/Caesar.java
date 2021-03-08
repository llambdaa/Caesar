package engine.caesar.core;

import engine.caesar.arg.AnnotatedArgument;
import engine.caesar.arg.Argument;
import engine.caesar.arg.Field;
import engine.caesar.arg.Scheme;
import engine.caesar.exception.IncoherentIndexException;
import engine.caesar.exception.IndexClashException;

import java.util.*;
import java.util.stream.Collectors;

public class Caesar {

    /**
     * Parsing rules are applied for different kinds of arguments.
     * Fields are anonymous values and are accessed using their index -
     * their rules underlying schemes are stored in @INDEXED_RULES.
     * Annotated arguments are identified arguments like flags - their
     * identifier is stored along with the arguments abstraction in @ANNOTATED_RULES.
     */
    private static List< Scheme > INDEXED_RULES;
    private static Map< String, AnnotatedArgument > ANNOTATED_RULES;

    /**
     * Fetching results differ depending on the rule set.
     * Fields are accessed via their index from @INDEXED_FETCH because they are anonymous and
     * cannot be found using a name.
     * Annotated arguments on the other hand are identified and hence their values can be
     * retrieved from @ANNOTATED_FETCH providing their name.
     */
    private static List< String > INDEXED_FETCH;
    private static Map< String, String > ANNOTATED_FETCH;

    /**
     * Calibrating the parser is an important step, because
     * it stores the parsing rules and validates them (checks for
     * inconsistencies).
     *
     * Inconsistencies could be:
     * 1) duplicate fixed indices
     * 2) incoherent indices (not directly following each other)
     */
    public static void calibrate( List< Argument > arguments ) {

        /* 1. Step: Retrieving field definitions and validating them.
         * ----------------------------------------------------------
         * Validation includes checking if indices are coherent.
         * If their are no duplicates, the sum of the fields indices must be:
         *
         *      sum = 0 + 1 + 2 + ... + n     [Little Gauß]
         *
         * when n = fields.size - 1.
         * If the sum doesn't equal to term, there must be duplicates.
         * In this case, an exception is raised.
         *
         * Sorting the collection of fields is necessary because otherwise
         * field definitions won't definitely land at their right index.
         * Furthermore it makes the process of finding errors like index clashes
         * easier.
         */
        List< Field > fields = arguments.stream()
                                        .filter( arg -> arg instanceof Field )
                                        .map( element -> (Field) element )
                                        .sorted( Comparator.comparingInt(Argument::getIndex) )
                                        .collect( Collectors.toList() );

        //Duplication check.
        try {

            int sum = fields.stream().mapToInt( Argument::getIndex ).sum();
            int n   = fields.size() - 1;

            //Indices are coherent.
            if ( sum == ( n*(n+1) / 2D ) ) {

                Caesar.INDEXED_RULES = fields.stream()
                                             .map( Field::getScheme )
                                             .collect( Collectors.toList() );

            } else {

                /* Index incoherence is detected by checking if the delta
                 * between two consecutive indices is greater than one.
                 * Since the collection is sorted, duplicate elements will
                 * consecutive elements - hence, duplications can also easily
                 * be detected by comparing the last element to the current one.
                 */
                int last = -1;
                for ( int i = 0; i < fields.size(); i++ ) {

                    int element = fields.get( i ).getIndex();
                    if ( last != element ) {

                        if ( element - last > 1 ) {

                            throw new IncoherentIndexException( element, last + 1 );

                        } else last = element;

                    } else throw new IndexClashException( i );

                }

            }

        } catch ( IndexClashException | IncoherentIndexException exception ) {

            exception.printStackTrace();

        }

    }

    public static void fetch( String ... arguments ) {

        //auswerten und schreiben

    }

    public static boolean isPresent( String identifier ) {

        return false;

    }

    public static void main( String ... arguments ) {

        List< Argument > args = new ArrayList<>();
        args.add( new Field( Scheme.INTEGER, 0 ) );
        args.add( new Field( Scheme.URI, 0 ) );
        Caesar.calibrate( args );

    }

}
