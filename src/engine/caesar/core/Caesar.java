package engine.caesar.core;

import engine.caesar.arg.*;
import engine.caesar.exception.*;
import engine.caesar.utils.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class Caesar {

    /** Program arguments can be separated into two major groups:
     *      1. Fields:
     *          These are anonymous arguments that have a fixed index at
     *          which they can be accessed.
     *
     *      2. Annotated Argument:
     *          These are the "real" and identified arguments, sometimes
     *          also referred to by the name of "annotated arguments"
     *          because they have an identifier or flag as an annotation before
     *          the actual argument value.
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
    private static Map< String, HashSet< AnnotatedArgument > > ARG_ALTERNATIVES;

    /** These collections store the parsing results. Values of fields are stored inside of @FIELDS,
     *  values of arguments however are stored inside of @ARGS along with their identifier.
     */
    private static List< String > FIELDS;
    private static Map< String, List< String > > ARGS;

    private static final String GROUP_EQUALS    = "=";
    private static final String GROUP_SEPARATOR = "(?<!\\\\);";

    /////////////////////////////////////////////////////////////////////
    //   _________                _____.__                             //
    //  \_   ___ \  ____   _____/ ____\__| ____  __ _________   ____   //
    //  /    \  \/ /  _ \ /    \   __\|  |/ ___\|  |  \_  __ \_/ __ \  //
    //  \     \___(  <_> )   |  \  |  |  / /_/  >  |  /|  | \/\  ___/  //
    //   \______  /\____/|___|  /__|  |__\___  /|____/ |__|    \___  > //
    //          \/            \/        /_____/                    \/  //
    //                                                                 //
    /////////////////////////////////////////////////////////////////////
    /** This method configures Caesar according to the given rules.
     *
     *  It separates different rule types (for fields and flags/groups),
     *  links related rules, alignes essentiality settings and checks
     *  for dependency clashes.
     */
    public static void configure( List< Argument > rules )
    throws IncoherentFieldsException, FieldClashException, DuplicateArgumentDefinitionException, DependencyClashExeption {

        Caesar.FIELD_CONFIG = Caesar.getFieldConfiguration( rules );
        Caesar.ARG_CONFIG   = Caesar.getArgumentConfiguration( rules );

        Caesar.crosslink();
        Caesar.ARG_ALTERNATIVES = Caesar.group();
        Caesar.alignEssentiality();
        Caesar.checkDependencyClashes();

    }

    /** This function separates out field rules and
     *  validates their claimed indices, so that index clashes
     *  or missing indices are detected.
     */
    private static List< Field > getFieldConfiguration( List< Argument > rules )
    throws FieldClashException, IncoherentFieldsException {

        List< Field > fields = rules.stream()
                                    .filter( arg -> arg instanceof Field )
                                    .map( arg -> ( Field ) arg )
                                    .sorted( Comparator.comparingInt( Field::getIndex ) )
                                    .collect( Collectors.toList() );

        /* The algorithm iterates over the filtered out fields.
         * It inspects their claimed index and compares it to
         * the index of the field before.
         * If there is a duplicate index, there is definitely an index
         * clash, which is why then an exception is thrown.
         * If, however, the indices have a greater delta than one, which
         * means that an index in between is not claimed by a field,
         * another exception is thrown.
         */
        int last = -1;
        for ( Field field : fields ) {

            int claim = field.getIndex();
            if ( claim == last ) {

                throw new FieldClashException( claim );

            } else if ( claim - last > 1 ) {

                throw new IncoherentFieldsException( claim, last + 1 );

            } else last = claim;

        }

        return fields;

    }

    /** This function separates out annotated argument rules and
     *  checks for duplicate definitions.
     *
     *  Note: Technically, mapping the rule to its identifier is
     *        redundant but allows for much faster search speeds.
     */
    private static Map< String, AnnotatedArgument > getArgumentConfiguration( List< Argument > rules )
    throws DuplicateArgumentDefinitionException {

        Map< String, AnnotatedArgument > result = new HashMap<>();
        for ( Argument rule : rules ) {

            if ( rule instanceof AnnotatedArgument ) {

                /* If the rule's identifier can be found in the
                 * already registered rules, there must be a
                 * duplication and then an exception is thrown.
                 */
                AnnotatedArgument annotated = ( AnnotatedArgument ) rule;
                String identifier = annotated.getIdentifier();
                if ( !result.containsKey( identifier ) ) {

                    result.put( identifier, annotated );

                } else throw new DuplicateArgumentDefinitionException( identifier );

            }

        }

        return result;

    }

    /** This method crosslinks rules by transforming unilateral connections
     *  between rules (a defined b as alternative) into bilateral connections
     *  (beware: Only direct connections, no implicit connections).
     *
     *  This process is needed because rules that define each other as
     *  alternatives are grouped together - without that bilateral connection
     *  making out the whole group is much harder.
     *
     */
    private static void crosslink() {

        Caesar.ARG_CONFIG.values().forEach( annotated ->  annotated.getAlternatives().forEach( other -> {

            List< AnnotatedArgument > alternatives = other.getAlternatives();
            if ( !alternatives.contains( annotated ) ) {

                alternatives.add( annotated );

            }

        } ) );

    }

    /** This function groups rules that defined each other
     *  as alternative respectively.
     *
     *  @return Collection of access optimized alternative groups.
     */
    private static Map< String, HashSet< AnnotatedArgument > > group() {

        Map< String, AnnotatedArgument >         annotated = ( HashMap< String, AnnotatedArgument > ) ( ( HashMap ) Caesar.ARG_CONFIG ).clone();
        Map< String, HashSet< AnnotatedArgument > > result = new HashMap<>();

        /* The algorithm takes the first element of @annotated and
         * groups it together with its alternatives.
         *
         * If an alternative is found, it is removed from @annotated, so that
         * in the next group building process, the first element is definitely
         * not part of that particular group that the found alternative belonged to.
         */
        while ( !annotated.isEmpty() ) {

            AnnotatedArgument                   first = ( AnnotatedArgument ) annotated.values().toArray()[ 0 ];
            HashSet< AnnotatedArgument > alternatives = new HashSet<>( Collections.singleton( first ) );
            Queue< AnnotatedArgument >          query = new LinkedList<>( first.getAlternatives() );

            /* When checking an annotated argument, its alternatives are put
             * into the queue, so that they themselves are checked later on.
             * This guarantees that at the end each element of the alternative
             * group has been processed.
             */
            annotated.remove( first.getIdentifier() );
            while ( !query.isEmpty() ) {

                AnnotatedArgument front = query.poll();
                String identifier       = front.getIdentifier();
                if ( annotated.containsKey( identifier ) ) {

                    annotated.remove( identifier );
                    alternatives.add( front );
                    query.addAll( front.getAlternatives() );

                }

            }

            /* At this point, all elements of the alternative group have been
             * found and it is ready to be returned.
             * For faster search, each element of the group itself is linked
             * with the whole group (minus itself) in the resulting hashmap.
             */
            alternatives.forEach( element -> {

                HashSet< AnnotatedArgument > group = ( HashSet< AnnotatedArgument > ) alternatives.clone();
                group.remove( element );

                result.put( element.getIdentifier(), group );

            } );

        }

        return result;

    }

    /** This method aligns the essentiality settings for
     *  alternative groups.
     *
     *  An alternative to an essential argument cannot be non-essential.
     *  Alternatives exclude each other and if the non-essential variant
     *  would be used, there would be no way to incorporate the essential
     *  argument without having an exception being thrown.
     *  The engine would either detect that two alternatives are in use
     *  at the same time or that an essential argument is missing.
     */
    private static void alignEssentiality() {

        Map< String, AnnotatedArgument > arguments = ( HashMap< String, AnnotatedArgument > ) ( ( HashMap ) Caesar.ARG_CONFIG ).clone();
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
            HashSet< AnnotatedArgument > group = ( HashSet< AnnotatedArgument > ) Caesar.ARG_ALTERNATIVES.get( first.getIdentifier() ).clone();
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

    /** This method checks for dependency clashes
     *  that occur when an argument has two dependencies
     *  that are respective alternatives and hence exclude
     *  each other - in that case the dependencies cannot
     *  coexist and requirements are never met.
     */
    private static void checkDependencyClashes()
    throws DependencyClashExeption {

        for ( Map.Entry< String, AnnotatedArgument > rule : Caesar.ARG_CONFIG.entrySet() ) {

            AnnotatedArgument argument = rule.getValue();
            HashSet< AnnotatedArgument > dependencies = new HashSet<>( argument.getDependencies() );

            for ( AnnotatedArgument dependency : dependencies ) {

                /* For each dependency of @rule, its alternatives are retrieved.
                 * If any of its alternatives can also be found in @rule's dependency
                 * collection, then @clashing is defined (as another dependency that is also
                 * an alternative to the currently inspected dependency) and an exception is thrown
                 * because this means that two alternative arguments (which exclude each other)
                 * are set as a dependency for @argument.
                 */
                HashSet< AnnotatedArgument > alternatives = Caesar.ARG_ALTERNATIVES.get( dependency.getIdentifier() );
                Optional< AnnotatedArgument> clashing = alternatives.stream().filter( dependencies::contains ).findAny();
                if ( clashing.isPresent() ) {

                    throw new DependencyClashExeption( dependency.getIdentifier(),
                                                       clashing.get().getIdentifier(),
                                                       argument.getIdentifier() );

                }

            }

        }

    }

    ////////////////////////////////////////////////////////
    //  __________                     .__                //
    //  \______   \_____ _______  _____|__| ____    ____  //
    //  |     ___/\__  \\_  __ \/  ___/  |/    \  / ___\  //
    //  |    |     / __ \|  | \/\___ \|  |   |  \/ /_/  > //
    //  |____|    (____  /__|  /____  >__|___|  /\___  /  //
    //                \/           \/        \//_____/    //
    //                                                    //
    ////////////////////////////////////////////////////////
    /** This method parses program arguments using predefined
     *  and sanitized rules.
     *
     *  Values are stored into their respective collection
     *  along with their preceding flag (if there is any).
     *
     *  Finally, essentiality and dependency checks are done.
     */
    public static void parse( List< String > arguments )
    throws SchemeMismatchException, EssentialArgumentMissingException, TooFewGroupValuesException, InvalidFlagException,
           ExcludedArgumentException, UnfulfilledArgumentDependencyException {

        Caesar.FIELDS = Caesar.getFieldValues( arguments );
        Caesar.ARGS   = Caesar.getArgumentValues( arguments.subList( Caesar.FIELDS.size(), arguments.size() ) );
        Caesar.checkEssentialsPresent();
        Caesar.checkDependencyFulfillment();

    }

    /** This function searches through program arguments for fields
     *  and validates its value with the field's scheme.
     *  If there are to few field values or the values have the wrong
     *  format, an exception is throws.
     *
     *  @return Collection of field values.
     */
    private static List< String > getFieldValues( List< String > fragments )
    throws SchemeMismatchException, EssentialArgumentMissingException {

        List< String > result = new ArrayList<>();
        for ( int i = 0; i < Caesar.FIELD_CONFIG.size(); i++ ) {

            /* If @fragments doesn't hold the value at this index,
             * the whole collection (and amount of fields) must be
             * smaller than the specified amount of fields.
             * Then, an exception is thrown, because there are too
             * few fields provided.
             */
            if ( i < fragments.size() ) {

                String value = fragments.get( i );
                if ( FIELD_CONFIG.get( i ).getScheme().applies( value ) ) {

                    result.add( value );

                } else throw new SchemeMismatchException( value );

            } else throw new EssentialArgumentMissingException( i );

        }

        return result;

    }

    /** This function searches through program arguments for flags
     *  and groups and validates their values using the groups schemes.
     *  If there are too few or too many values or if the values
     *  have the wrong format, an exception is thrown.
     *
     *  @return Collection of argument flags and their values.
     */
    private static Map< String, List< String > > getArgumentValues( List< String > fragments )
    throws ExcludedArgumentException, InvalidFlagException, SchemeMismatchException, TooFewGroupValuesException {

        Map< String, List< String > > result = new HashMap<>();
        if ( Caesar.ARG_CONFIG.size() > 0 ) {

            /* The field @parent is defined if previously its flag has been
             * detected. The group itself is considered the 'parent' of the
             * arguments to come.
             */
            Group parent = null;
            List< String > values = null;
            int expected = 0;
            AnnotatedArgument candidate;

            /* Each iteration yields another fragment (part of the program
             * arguments) which could indicate another argument coming or is one
             * value of the currently inspected argument.
             */
            while ( !fragments.isEmpty() ) {

                /* If @fragment contains @Caesar.GROUP_EQUALS, it is
                 * in the @Format.EQUALS format.
                 * By splitting at the "equals separator", the identifier
                 * and values can be regained - @fragment is reassigned
                 * to the identifier and the values are put back into
                 * @fragments for further processing.
                 *
                 * This allows for seamless transformation to @FORMAT.WHITESPACE
                 * without touching the rest of the parsing algorithm.
                 */
                String fragment = fragments.remove( 0 );
                if ( fragment.contains( Caesar.GROUP_EQUALS ) ) {

                    String[] parts = StringUtils.split( fragment, Caesar.GROUP_EQUALS );
                    fragment = parts[ 0 ];
                    fragments.addAll( 0, Arrays.asList( parts[ 1 ].split( Caesar.GROUP_SEPARATOR ) ) );

                }

                candidate = Caesar.ARG_CONFIG.get( fragment );

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
                        Optional< AnnotatedArgument > excluder = Caesar.ARG_ALTERNATIVES.getOrDefault( fragment, new HashSet<>() )
                                                                                        .stream()
                                                                                        .filter( alt -> result.containsKey( alt.getIdentifier() ) )
                                                                                        .findAny();
                        if ( excluder.isEmpty() ) {

                            if ( candidate instanceof Group ) {

                                parent = ( Group ) candidate;
                                expected = parent.getSchemes().size();

                                /* If @parent is group but the group itself has no
                                 * expected children, the group might also be considered
                                 * a flag (because technically it is) and hence
                                 * @parent is set back to null for further parsing.
                                 */
                                if ( expected > 0 ) {

                                    values = new ArrayList<>();

                                } else {

                                    parent = null;
                                    result.put( fragment, null );

                                }

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

        }

        return result;

    }

    /** This method checks whether all essential arguments or
     *  any of its alternatives are present.
     */
    private static void checkEssentialsPresent()
    throws EssentialArgumentMissingException {

        for ( AnnotatedArgument rule : Caesar.ARG_CONFIG.values() ) {

            /* Each essential rule must be represented in the program arguments,
             * either by its own identifier or by its alternatives.
             */
            String identifier = rule.getIdentifier();
            if ( rule.isEssential() && !Caesar.ARGS.containsKey( identifier ) ) {

                /* It is sufficient if any of the rule's alternatives can be found in
                 * the parsed arguments collection, because it is not allowed to have
                 * multiple alternatives present in the program arguments at the same
                 * time.
                 * If neither the argument itself nor any of its alternatives is found,
                 * an exception is thrown.
                 */
                boolean represented = Caesar.ARG_ALTERNATIVES.entrySet().stream()
                                                                        .anyMatch( alias -> Caesar.ARGS.containsKey( alias.getKey() ) );
                if ( !represented ) {

                    throw new EssentialArgumentMissingException( identifier );

                }

            }

        }

    }

    /** This method checks whether all dependency requirements
     *  for each argument have been met.
     */
    private static void checkDependencyFulfillment()
    throws UnfulfilledArgumentDependencyException {

        for ( String argument : Caesar.ARGS.keySet() ) {

            /* For each argument, its dependencies are retrieved.
             * If any of the dependencies cannot be found in
             * @Caesar.ARGS, meaning the argument has never been
             * provided, an exception is thrown.
             */
            List< String > dependencies = Caesar.ARG_CONFIG.get( argument ).getDependencies().stream()
                                                                                             .map( AnnotatedArgument::getIdentifier )
                                                                                             .collect( Collectors.toList() );
            for ( String dependency : dependencies ) {

                if ( !Caesar.ARGS.containsKey( dependency ) ) {

                    throw new UnfulfilledArgumentDependencyException( argument, dependency );

                }

            }

        }

    }

    //////////////////////////////////////////////////
    //                                              //
    //    _____                                     //
    //   /  _  \   ____  ____  ____   ______ ______ //
    //  /  /_\  \_/ ___\/ ___\/ __ \ /  ___//  ___/ //
    // /    |    \  \__\  \__\  ___/ \___ \ \___ \  //
    // \____|__  /\___  >___  >___  >____  >____  > //
    //         \/     \/    \/    \/     \/     \/  //
    //                                              //
    //////////////////////////////////////////////////

    /** This function retrieves the field value at
     *  the given index.
     *
     *  @return Optional of value (in case it is non-existent).
     */
    public static Optional< String > getFieldValue( int index ) {

        return Optional.ofNullable( Caesar.FIELDS.get( index ) );

    }

    /** This function retrieves the argument values
     *  of the argument with the given identifier.
     *
     *  @return Optional of values (in case there are none).
     */
    public static Optional< List< String > > getArgumentValues( String identifier ) {

        return Optional.ofNullable( Caesar.ARGS.get( identifier ) );

    }

    /** This function checks whether arguments
     *  with the given identifiers have been parsed.
     *
     *  @return whether all of the identified arguments
     *          are present.
     */
    public static boolean isPresent( String ... arguments ) {

        for ( String argument : arguments ) {

            if ( !Caesar.ARGS.containsKey( argument ) ) {

                return false;

            }

        }

        return true;

    }

    /** This function searches for all directly dependent
     *  arguments of the identified argument.
     *
     *  @return Collection of directly dependent arguments
     *          mapped to their values.
     */
    public static Map< String, List< String > > getDependentArguments( String dependencyIdentifier ) {

        Map< String, List< String > > result = new HashMap<>();
        if ( Caesar.ARGS.containsKey( dependencyIdentifier ) ) {

            AnnotatedArgument dependency = Caesar.ARG_CONFIG.get( dependencyIdentifier );

            /* If another parsed argument depends on @dependency
             * (so if @dependency is contained within the argument's
             * dependency collection), it is stored as a dependent
             * argument of @dependency along with its values.
             */
            Caesar.ARGS.forEach( ( identifier, values ) -> {

                AnnotatedArgument rule = Caesar.ARG_CONFIG.get( identifier );
                if ( rule.getDependencies().contains( dependency ) ) {

                    result.put( identifier, values );

                }

            } );

        } else new InvalidArgumentException( dependencyIdentifier, "Argument with this identifier could not be found." ).printStackTrace();

        return result;

    }

    /** This function searches for all dependencies
     *  of the identified argument.
     *
     *  @return Collection of dependencies
     *          mapped to their values.
     */
    public static Map< String, List< String > > getDependencies( String dependentIdentifier ) {

        Map< String, List< String > > result = new HashMap<>();
        if ( Caesar.ARGS.containsKey( dependentIdentifier ) ) {

            AnnotatedArgument dependent = Caesar.ARG_CONFIG.get( dependentIdentifier );
            dependent.getDependencies().forEach( dependency -> {

                String identifier = dependency.getIdentifier();
                result.put( identifier, Caesar.ARGS.get( identifier ) );

            } );

        } else new InvalidArgumentException( dependentIdentifier, "Argument with this identifier could not be found." ).printStackTrace();

        return result;

    }

    public static void main( String ... arguments ) {

        List< String > args = new ArrayList<>();
        args.add( "-a" );
        args.add( "20" );
        args.add( "-b" );
        args.add( "30" );

        List< Argument > config = new ArrayList<>();
        Group a = new Group( true, "-a", Format.WHITESPACE, Scheme.INTEGER, null, null );
        Group b = new Group( false, "-b", Format.WHITESPACE, Scheme.INTEGER, null, Arrays.asList( a ) );
        config.add( a );
        config.add( b );

        try {

            Caesar.configure( config );
            Caesar.parse( args );

        } catch ( Exception exception ) {

            exception.printStackTrace();

        }

    }

}
