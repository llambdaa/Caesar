package engine.caesar.core;

import engine.caesar.arg.*;
import engine.caesar.exception.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Caesar {

    /** Program arguments can be separated into two major groups:
     *      1. Fields: These are anonymous arguments that have a fixed index at
     *                 which they can be accessed.
     *      2. Arguments: These are the "real" and identified arguments, sometimes
     *                    also referred to by the name of "annotated arguments"
     *                    because they have an identifier or flag as an annotation before
     *                    the actual argument value.
     *
     *  Since these types are accessed in to very different manners (fields by indices
     *  and arguments by identifier), they are stored in separate data structures.
     *
     *  For the sake of computational simplicity for the complex process of parsing
     *  program arguments, after the main configuration, aliased arguments are grouped
     *  together and stored in a third structure, although it technically is redundant.
     *  But the form allows for faster comparison because aliases of a given argument
     *  don't have to be searched for each time they are to be used.
     */
    private static List< Field > FIELD_CONFIG;
    private static Map< String, AnnotatedArgument > ARG_CONFIG;
    private static Map< String, HashSet< AnnotatedArgument > > ARG_ALIASES;

    /** These collections store the parsing results. Values of fields are stored inside of @FIELDS,
     *  values of arguments however are stored inside of @ARGS along with their identifier.
     */
    private static List< String > FIELDS;
    private static Map< String, List< String > > ARGS;

    /** Caesar doesn't know how to parse the program arguments without
     *  a definite rule set - it must therefore first be configured before usage.
     */
    public static void configure( List< Argument > configuration ) {

        /* -------------------------------------------------------------
         * 1. Step: Finding field declarations and validating them.
         * ------------------------------------------------------------- */
        Caesar.FIELD_CONFIG = Caesar.getFieldConfigurations( configuration );

        /* -------------------------------------------------------------
         * 2. Step: Finding argument declarations and validating them.
         * ------------------------------------------------------------- */
        Caesar.ARG_CONFIG = Caesar.getArgumentConfigurations( configuration );

        /* -------------------------------------------------------------
         * 3. Step: Crosslinking related arguments.
         * ------------------------------------------------------------- */
        Caesar.crosslink( Caesar.ARG_CONFIG );

        /* -------------------------------------------------------------
         * 4. Step: Grouping together alternative or aliased arguments.
         * ------------------------------------------------------------- */
        Caesar.ARG_ALIASES = Caesar.groupAliases( Caesar.ARG_CONFIG );

        /* -------------------------------------------------------------
         * 5. Step: Aligning essentiality over whole alias groups.
         * ------------------------------------------------------------- */
        Caesar.alignEssentiality( Caesar.ARG_CONFIG, Caesar.ARG_ALIASES );

        /* -------------------------------------------------------------
         * 6. Step: Checking for dependency clashes.
         * ------------------------------------------------------------- */
        Caesar.checkDependencies( Caesar.ARG_CONFIG, Caesar.ARG_ALIASES );

    }

    /** This function filters out indexed field arguments from the given
     *  collection and then validates them for index coherence before returning.
     */
    private static List< Field > getFieldConfigurations( List< Argument > fragments ) {

        List< Field > fields = fragments.stream()
                                        .filter( arg -> arg instanceof Field )
                                        .map( arg -> ( Field ) arg )
                                        .sorted( Comparator.comparingInt( Field::getIndex ) )
                                        .collect( Collectors.toList() );

        /* The algorithm iterates over the filtered out fields.
         * It inspects their claimed index and compares it to
         * the index of the field before.
         * If there is a duplicate index, there is definitely an index
         * clash, which is why an exception is thrown.
         * If, however, the indices have a greater delta than one, which
         * means that an index in between is not claimed by a field,
         * another exception is thrown.
         */
        try {

            int last = -1;
            for ( Field field : fields ) {

                int claim = field.getIndex();
                if ( claim == last ) {

                    throw new FieldClashException( claim );

                } else if ( claim - last > 1 ) {

                    throw new IncoherentFieldsException( claim, last + 1 );

                } else last = claim;

            }

        } catch ( FieldClashException | IncoherentFieldsException exception ) {

            exception.printStackTrace();

        }

        /* No field incoherence or index clash has been detected,
         * so the collection can be returned.
         */
        return fields;

    }

    /** This function filters out annotated arguments from the given
     *  collection and then searches for duplicates before returning.
     *
     *  Returning a map of an annotated argument with its identifier
     *  is redundant because the object holds the identifier itself
     *  but this also allows for theoretical faster access speeds
     *  when working with the parsed arguments.
     */
    private static Map< String, AnnotatedArgument > getArgumentConfigurations( List< Argument > fragments ) {

        Map< String, AnnotatedArgument > result = new HashMap<>();
        fragments.forEach( arg -> {

            try {

                if ( arg instanceof AnnotatedArgument ) {

                    AnnotatedArgument annotated = ( AnnotatedArgument ) arg;
                    String identifier = annotated.getIdentifier();
                    if ( !result.containsKey( identifier ) ) {

                        result.put( identifier, annotated );

                    } else throw new DuplicateArgumentDefinitionException( identifier );

                }

            } catch ( DuplicateArgumentDefinitionException exception ) {

                exception.printStackTrace();

            }

        } );

        return result;

    }

    /** This method crosslinks arguments by declaring them their respective
     *  alternative or alias if at least one of them has declared the other
     *  one as such - this is needed, so that arguments know of each other
     *  as an alias.
     *
     *  After the process an argument does only know about other arguments
     *  that are directly linked to it but not about the ones that are implicitly
     *  connected.
     *  Example: a (not linked) and b (linked to a)
     *
     *  If b knows about a because they are alternatives to each other, than a
     *  should also know about b as an alternative.
     *  Basically, unilateral connections are made bilateral.
     *  This ensures, that if you take any element from the hypothetical group,
     *  that this element knows about at least one other element of that group.
     *  This allows the group finding algorithm to wander around these direct
     *  connections to make out the whole group.
     */
    private static void crosslink( Map< String, AnnotatedArgument > arguments ) {

        arguments.values().forEach( arg ->  arg.getAlternatives().forEach( other -> {

            List< AnnotatedArgument > alternatives = other.getAlternatives();
            if ( !alternatives.contains( arg ) ) {

                alternatives.add( arg );

            }

        } ) );

    }

    /** This function groups together arguments that alias each other.
     *  Found groups are put into @Caesar.ALIASES and are later used
     *  to detect e.g. dependency clashes.
     */
    private static Map< String, HashSet< AnnotatedArgument > > groupAliases( Map< String, AnnotatedArgument > source ) {

        Map< String, AnnotatedArgument >         arguments = ( HashMap< String, AnnotatedArgument > ) ( ( HashMap ) source ).clone();
        Map< String, HashSet< AnnotatedArgument > > result = new HashMap<>();

        /* The algorithm takes the first element of @arguments and
         * groups it together with its aliases.
         * If an alias is found, it is removed from @arguments, so that
         * in the next major iteration, the first element is an alias
         * in another group.
         */
        while ( !arguments.isEmpty() ) {

            AnnotatedArgument              first = ( AnnotatedArgument ) arguments.values().toArray()[ 0 ];
            HashSet< AnnotatedArgument > aliases = new HashSet<>( Collections.singleton( first ) );
            Queue< AnnotatedArgument >     query = new LinkedList<>( first.getAlternatives() );

            /* When checking an annotated argument, its alternative arguments
             * are put on the queue, so that they themselves are checked later on.
             * This guarantees that at the end each element of the alias group
             * has been processed.
             */
            arguments.remove( first.getIdentifier() );
            while ( !query.isEmpty() ) {

                AnnotatedArgument front = query.poll();
                String identifier       = front.getIdentifier();
                if ( arguments.containsKey( identifier ) ) {

                    arguments.remove( identifier );
                    aliases.add( front );
                    query.addAll( front.getAlternatives() );

                }

            }

            /* At this point, all elements of the alias group have been
             * found and it is ready to be outputted.
             * For faster search, each element of the group itself is linked
             * with the whole group (minus itself) in the resulting hashmap.
             */
            aliases.forEach( alias -> {

                HashSet< AnnotatedArgument > alternatives = ( HashSet< AnnotatedArgument > ) aliases.clone();
                alternatives.remove( alias );

                result.put( alias.getIdentifier(), alternatives );

            } );

        }

        return result;

    }

    /** This method aligns essentiality values for each argument
     *  in an alias group.
     *
     *  This is an important step because an alternative to an essential argument
     *  cannot be non-essential. Alternative arguments exclude each other and if
     *  the non-essential argument would be used instead of the essential one,
     *  the engine would detect that an essential argument is missing.
     *  But at the same time, the essential argument cannot be used
     *  because it is excluded due to the non-essential argument being in use.
     */
    private static void alignEssentiality( Map< String, AnnotatedArgument > source, Map< String, HashSet< AnnotatedArgument > > aliases ) {

        Map< String, AnnotatedArgument > arguments = ( HashMap< String, AnnotatedArgument > ) ( ( HashMap ) source ).clone();
        while ( arguments.size() > 0 ) {

            /* In each iteration the first element can be accessed and is surely an element of a group
             * that hasn't been inspected yet because in each preceding iteration the whole alias group
             * of the former first element has been removed.
             *
             * Since the alias group stored along with the first element doesn't contain the element itself
             * for logical and architectural reasons, the group collection must be cloned first so that
             * the element itself can be added to it for further calculations without affecting the source collection.
             */
            AnnotatedArgument            first = ( AnnotatedArgument ) arguments.values().toArray()[ 0 ];
            HashSet< AnnotatedArgument > group = ( HashSet< AnnotatedArgument > ) aliases.get( first.getIdentifier() ).clone();
            group.add( first );

            /* Essential is true if any of the elements of the alias group
             * itself is essential.
             * If it is indeed true, each argument must be declared essential.
             */
            boolean essential = group.stream().anyMatch( Argument::isEssential );
            group.forEach( arg -> {

                if ( essential ) {

                    arg.setEssential( true );

                }

                /* After evaluation and possible declaration of essentiality,
                 * each element of the alias group can be deleted from the map
                 * because otherwise the calculation would be done again as
                 * soon as the iteration finds this element.
                 */
                arguments.remove( arg.getIdentifier() );

            } );

        }

    }

    private static void checkDependencies( Map< String, AnnotatedArgument > config, Map< String, HashSet< AnnotatedArgument > > aliases ) {

        for ( Map.Entry< String, AnnotatedArgument > element : config.entrySet() ) {

            String identifier = element.getKey();
            AnnotatedArgument argument = element.getValue();

            /* The actual references to the argument dependencies are not needed.
             * For faster search (only the identifiers are needed), the dependency
             * collection is transformed into an identifier HashSet.
             */
            HashSet< AnnotatedArgument > dependencies = new HashSet<>( argument.getDependencies() );
            for ( AnnotatedArgument dependency : dependencies ) {

                try {

                    /* For each dependency of @argument, its alternatives are retrieved.
                     * If any of its alternatives can also be found in @argument's dependency
                     * collection, then @clashing is defined (as another dependency that is also
                     * an alternative to the currently inspected dependency) and an exception is thrown
                     * because this means that two alternative arguments (which exclude each other)
                     * are set as a dependency for @argument.
                     */
                    HashSet< AnnotatedArgument > alternatives = aliases.get( dependency.getIdentifier() );
                    Optional< AnnotatedArgument> clashing = alternatives.stream().filter( dependencies::contains ).findAny();
                    if ( clashing.isPresent() ) {

                        throw new DependencyClashExeption( dependency.getIdentifier(),
                                                           clashing.get().getIdentifier(),
                                                           argument.getIdentifier() );

                    }

                } catch ( DependencyClashExeption exception ) {

                    exception.printStackTrace();

                }

            }

        }

    }

//    public static void evaluate( String ... arguments ) {
//
//        /* 3. Step: Evaluating dependencies and their fulfillment.
//         * ----------------------------------------------------------
//         * Annotated arguments can have dependencies which must be evaluated.
//         * If an argument doesn't have its dependencies fulfilled, an
//         * exception is thrown.
//         */
//        try {
//
//            for ( String argument : ANNOTATED_FETCH.keySet() ) {
//
//                List< AnnotatedArgument > dependencies = ANNOTATED_RULES.get( argument ).getDependencies();
//                for ( AnnotatedArgument dependency : dependencies ) {
//
//                    String identifier = dependency.getIdentifier();
//                    if ( !ANNOTATED_FETCH.containsKey( identifier ) ) {
//
//                        throw new UnfulfilledArgumentDependencyException( argument, identifier );
//
//                    }
//
//                }
//
//            }
//
//        } catch ( UnfulfilledArgumentDependencyException exception ) {
//
//            exception.printStackTrace();
//
//        }
//
//        /* x. Step: Checking if all essential arguments are provided.
//         * ----------------------------------------------------------
//         */
//        try {
//
//            for ( Map.Entry< String, AnnotatedArgument > entry : ANNOTATED_RULES.entrySet() ) {
//
//                if ( entry.getValue().isEssential() && !ANNOTATED_FETCH.containsKey( entry.getKey() ) ) {
//
//                    throw new EssentialArgumentMissingException( entry.getKey() );
//
//                }
//
//            }
//
//        } catch ( EssentialArgumentMissingException exception ) {
//
//            exception.printStackTrace();
//
//        }
//
//    }

    public static void process( List< String > fragments ) {

        /* ------------------------------------------------------------------
         * 1. Step: Getting field values and comparing them against schemes.
         * ------------------------------------------------------------------ */
        Caesar.FIELDS = Caesar.getFields( Caesar.FIELD_CONFIG, fragments );

        /* ------------------------------------------------------------------
         * 2. Step: Getting argument values and comparing them against schemes.
         * ------------------------------------------------------------------ */
        Caesar.ARGS = Caesar.getArguments( Caesar.ARG_CONFIG,
                                           Caesar.ARG_ALIASES,
                                           fragments.subList( Caesar.FIELDS.size(), fragments.size() ) );

    }

    /** This function scans through the set of program arguments, searches for fields
     *  and validates their values with help of their respective schemes.
     *  If all values have the right format, the collection of values is returned.
     */
    private static List< String > getFields( List< Field > config, List< String > fragments ) {

        List< String > result = new ArrayList<>();
        try {

            for ( int i = 0; i < config.size(); i++ ) {

                /* If fragments doesn't hold the value at this index,
                 * the whole collection (and amount of fields) must be
                 * smaller than the specified amount of fields.
                 * Then, an exception is thrown, because there are too
                 * few fields provided.
                 */
                if ( i < fragments.size() ) {

                    String candidate = fragments.get( i );
                    if ( config.get( i ).getScheme().applies( candidate ) ) {

                        result.add( candidate );

                    } else throw new SchemeMismatchException( candidate );

                } else throw new EssentialArgumentMissingException( i );

            }

        } catch ( EssentialArgumentMissingException | SchemeMismatchException exception ) {

            exception.printStackTrace();

        }

        return result;

    }

    /** This function scans through the set of program arguments, searches for arguments
     *  and their values and validates them with help of the respective schemes.
     *  If all values have the right format, the collection of annotated arguments is returned.
     */
    private static Map< String, List< String > > getArguments( Map< String, AnnotatedArgument > config, Map< String, HashSet< AnnotatedArgument > > aliases, List< String > fragments ) {

        Map< String, List< String > > result = new HashMap<>();
        if ( config.size() > 0 ) {

            try {

                /* The field @parent is defined if previously its flag has been
                 * detected. The group itself is considered the 'parent' of the
                 * arguments to come.
                 */
                Group parent = null;
                List< String > values = null;
                int expected = 0;
                AnnotatedArgument candidate;

                /* Each iteration yields another fragment (basically part of the program
                 * arguments) which could indicate another argument coming or is one
                 * value of the currently inspected argument.
                 */
                for ( String fragment : fragments ) {

                    candidate = config.get( fragment );
                    if ( parent == null ) {

                        /* If @parent is defined, the algorithm expects another argument,
                         * so either a flag or a group.
                         * However, if @candidate is undefined, it means that the current
                         * fragment is invalid and hence an exception is thrown.
                         */
                        if ( candidate != null ) {

                            /* If any alternative or alias can be found within the resulting
                             * collection, the current argument is automatically excluded.
                             * When @excluder is defined, then there are two arguments present
                             * that exclude each other and an exception is thrown.
                             */
                            Optional< AnnotatedArgument > excluder = aliases.get( fragment )
                                                                            .stream()
                                                                            .filter( alt -> result.containsKey( alt.getIdentifier() ) )
                                                                            .findAny();
                            if ( excluder.isEmpty() ) {

                                if ( candidate instanceof Group ) {

                                    parent = ( Group ) candidate;
                                    expected = parent.getSchemes().size();
                                    values = new ArrayList<>();

                                } else result.put( fragment, null );

                            } else throw new ExcludedArgumentException( fragment, excluder.get().getIdentifier() );

                        } else throw new InvalidFlagException( fragment );

                    } else {

                        /* In case @candidate is defined, an exception is thrown
                         * because it means that another flag has been read before
                         * finishing reading the values for @parent.
                         */
                        if ( candidate == null ) {

                            /* If the scheme applies to the fragment, then it is a valid
                             * argument value for @parent and therefore is stored in @values.
                             * However, when @values has stored all expected values,
                             * then @parent is set back to null, so that the next iteration
                             * can start reading a new flag without causing an exception.
                             */
                            Scheme scheme = parent.getSchemes().get( values.size() );
                            if ( scheme.applies( fragment ) ) {

                                values.add( fragment );
                                if ( values.size() == expected ) {

                                    result.put( parent.getIdentifier(), values );
                                    parent = null;

                                }

                            } else throw new SchemeMismatchException( fragment );

                        } else throw new TooFewGroupValuesException( parent.getIdentifier(), values.size(), expected );

                    }

                }

                /* If after iterating over the program arguments, @parent is still defined,
                 * it means, that parsing wasn't done because there were still some argument
                 * values missing.
                 */
                if ( parent != null ) {

                    throw new TooFewGroupValuesException( parent.getIdentifier(), values.size(), expected );

                }

            } catch ( InvalidFlagException | ExcludedArgumentException | TooFewGroupValuesException | SchemeMismatchException exception ) {

                exception.printStackTrace();

            }

        }

        return result;

    }

    public static Optional< String > getValue( int index ) {

        return Optional.ofNullable( Caesar.FIELDS.get( index ) );

    }

    public static Optional< List< String > > getValues( String argument ) {

        return Optional.ofNullable( Caesar.ARGS.get( argument ) );

    }

    public static boolean isPresent( String argument ) {

        return Caesar.ARGS.containsKey( argument );

    }

    public static void main( String ... arguments ) {

        List< String > args = new ArrayList<>();

        List< Argument > config = new ArrayList<>();
        Group a = new Group( true, "-a", Scheme.INTEGER, null, null );
        Group b = new Group( true, "-b", Arrays.asList( Scheme.INTEGER, Scheme.INTEGER ), Collections.singletonList( a ), null );
        Group c = new Group( true, "-c", Scheme.PASS_ALL, null, Arrays.asList( a, b ) );
        config.add( a );
        config.add( b );
        config.add( c );

        Caesar.configure( config );
        Caesar.process( args );
//        System.out.println( Caesar.getValues( "-d" ).orElse( new ArrayList<>() ).size() );

    }

}
