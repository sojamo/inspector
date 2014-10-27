package sojamo.inspector;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.event.Event;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

@SuppressWarnings( "unchecked" ) public final class Inspector {

	private final PApplet p;
	private final Map< String , Map > listeners;
	private PGraphics pg;
	private int x = 20;
	private int y = 20;
	private float oy = 0;
	private float ny = 0;
	private int len = 0;
	private int textLeading = 13;
	private int textSize = 12;
	private int spacing = 20;
	private PFont font;
	private final Map< String , Map > graphs = new HashMap( );
	private final Map< String , Map > display = new HashMap( );
	private final Map< String , InspectorItem > items = new HashMap< String , InspectorItem >( );
	private boolean show = true;

	public Inspector( PApplet thePApplet ) {
		p = thePApplet;
		pg = p.createGraphics( 300 , 200 );
		listeners = new LinkedHashMap< String , Map >( );
		font = p.createFont( "" , textSize );
		p.registerMethod( "pre" , this );
		p.registerMethod( "draw" , this );
		p.registerMethod( "mouseEvent" , this );
		p.registerMethod( "keyEvent" , this );
		graphs.put( "single-line" , null );
		init( );
	}

	public Inspector setSpacing( int theValue ) {
		spacing = theValue;
		return this;
	}

	public Inspector textLeading( int theValue ) {
		textLeading = theValue;
		return this;
	}

	public Inspector textFont( PFont theFont ) {
		font = theFont;
		return this;
	}

	public Inspector textSize( int theValue ) {
		textSize = theValue;
		return this;
	}

	public Inspector setSize( int theWidth , int theHeight ) {
		pg = p.createGraphics( theWidth , theHeight );
		return this;
	}

	public Inspector setPosition( int theX , int theY ) {
		x = theX;
		y = theY;
		return this;
	}

	public final void pre( ) {
		if ( !show ) {
			return;
		}
		for ( Map.Entry< String , Map > entry : listeners.entrySet( ) ) {
			Object now = entry.getValue( ).get( "now" );
			Object then = entry.getValue( ).get( "then" );
			Object field = entry.getValue( ).get( "field" );
			Object owner = entry.getValue( ).get( "owner" );
			Object path = entry.getValue( ).get( "path" );

			try {
				now = ( ( Field ) field ).get( owner );
			} catch ( IllegalArgumentException e ) {
				e.printStackTrace( );
			} catch ( IllegalAccessException e ) {
				e.printStackTrace( );
			}

			if ( !now.equals( then ) ) {
				entry.getValue( ).put( "now" , now );
				entry.getValue( ).put( "then" , now );
				entry.getValue( ).put( "highlight" , 1 );
				/* TODO ! how about an observer notify here? #enhance ^2 */
			}
		}
		p.pushMatrix( );
	}

	public void draw( ) {
		len = 0;
		oy += ( ny - oy ) * 0.15;
		if ( !show ) {
			return;
		}

		p.popMatrix( );

		p.pushMatrix( );
		pg.beginDraw( );
		pg.background( 0 , 80 );
		pg.noStroke( );
		pg.textFont( font );
		pg.textSize( textSize );
		pg.textLeading( textLeading );
		pg.translate( 20 , oy );
		for ( Map.Entry< String , Map > entry : listeners.entrySet( ) ) {
			pg.pushStyle( );
			float highlight = f( entry.getValue( ).get( "highlight" ) , 0 );
			if ( highlight > 0.01 ) {
				highlight += ( 0 - highlight ) * 0.1f;
				entry.getValue( ).put( "highlight" , highlight );
				pg.fill( 255 , highlight * 255 );
				pg.pushMatrix( );
				pg.translate( -12 , spacing / 2 + 4 );
				pg.rect( 0 , 0 , 4 , 4 );
				pg.popMatrix( );
			}

			Map m = entry.getValue( );

			boolean isType = m.containsKey( "type" );
			if ( isType ) {
				isType = items.containsKey( m.get( "type" ) );
			}

			int n = 0;

			if ( m.get( "now" ) instanceof List ) {
				if ( !isType ) {
					n = items.get( "default-list" ).draw( pg , m );
				} else {
					n += items.get( m.get( "type" ) ).draw( pg , m );
				}
			} else {
				if ( !isType ) {
					n = items.get( "default-item" ).draw( pg , m );
				} else {
					n += items.get( m.get( "type" ) ).draw( pg , m );
				}
			}

			len += n;

			pg.translate( 0 , n );
			pg.popStyle( );
		}

		pg.endDraw( );
		p.image( pg , x , y );
		p.popMatrix( );

	}

	public void mouseEvent( MouseEvent theEvent ) {
		if ( theEvent.getX( ) > x && theEvent.getX( ) < pg.width && theEvent.getX( ) > y && theEvent.getX( ) < pg.height ) {
			if ( theEvent.getAction( ) == MouseEvent.DRAG || theEvent.getAction( ) == MouseEvent.WHEEL ) {
				float dif = ( theEvent.getAction( ) == MouseEvent.DRAG ) ? p.mouseY - p.pmouseY : theEvent.getCount( );
				ny += dif;
				ny = p.min( 0 , ny );
				ny = p.max( pg.height - len - spacing , ny );
			}
		}
		println( len , pg.height );
	}

	public void keyEvent( KeyEvent theEvent ) {
		final int keyCode = 73; // i
		if ( theEvent.getKeyCode( ) == keyCode && theEvent.getModifiers( ) == Event.CTRL && theEvent.getAction( ) == KeyEvent.RELEASE ) {
			show = !show;
		}
	}

	public Inspector change( String thePath , Object ... theParams ) {
		if ( listeners.containsKey( thePath ) ) {
			Map m = listeners.get( thePath );
			m.putAll( toMap( theParams ) );
			println( m );
		}

		return this;
	}

	private String format( Object theValue ) {
		boolean number = theValue instanceof Number;
		if ( number ) {
			if ( theValue instanceof Float || theValue instanceof Double ) {
				return String.format( "%.1f" , theValue );
			}
		}
		return theValue.toString( );
	}

	public Inspector add( final String ... theFieldNames ) {
		return add( p , theFieldNames );
	}

	public Inspector add( final Object theObject , final String ... theNames ) {
		for ( final String name : theNames ) {
			if ( name.indexOf( " " ) != -1 ) {
				test( name );
			} else {
				evaluate( theObject , name );
			}
		}
		return this;
	}

	// TODO ! combine with evaluate #fix ^2 
	private void test( String thePath ) {

		LinkedList< String > path = new LinkedList( Arrays.asList( thePath.split( " " ) ) );
		if ( path.size( ) < 2 ) {
			return;
		}

		Object member = null;

		// "t l size";
		// "t l 0";
		// "t l 0 getValue";
		// "t l 0..10 getValue";
		// "t l getValue";
		// "t m hello getValue"

		member = p;
		final String invoke = path.pollLast( );

		for ( String s : path ) {
			Object o = evaluateMember( member , s );
			if ( o instanceof Field ) {
				System.out.println( "Field " + ( ( Field ) o ).getType( ) );
				try {
					member = ( ( Field ) o ).get( member );
				} catch ( IllegalArgumentException e ) {
					println( e );
				} catch ( IllegalAccessException e ) {
					println( e );
				}
			} else if ( o instanceof Method ) {
				println( "Method " + ( ( Method ) o ).getReturnType( ) );
			} else {
				System.out.println( "Err." + member.getClass( ) );
				if ( member instanceof List ) {
					System.out.println( "isList: " + ( ( List ) member ).get( 0 ) );
				} else if ( member instanceof Map ) {

				}
			}
		}

		Object value = null;

		addListener( ( Field ) evaluateMember( member , invoke ) , member , thePath , value );

	}

	private void addListener( final Field theField , final Object theOwner , final String thePath , final Object theValue ) {
		if ( notNull( theField , theOwner , thePath ) ) {

			Map< String , Object > m = new HashMap( );
			m.put( "owner" , theOwner );
			m.put( "path" , thePath );
			m.put( "then" , theValue );
			m.put( "now" , theValue );
			m.put( "field" , theField );
			m.put( "highlight" , 0 );
			listeners.put( thePath , m );

		}
	}

	private final Object evaluateMember( final Object theObject , final String theName ) {
		Class< ? > c = theObject.getClass( );
		while ( c != null ) {
			try {
				final Field field = c.getDeclaredField( theName );
				field.setAccessible( true );
				return field;
			} catch ( Exception e ) {
				try {
					final Method method = c.getMethod( theName , new Class< ? >[] { } );
					return method;
				} catch ( SecurityException e1 ) {
					System.out.println( e );
				} catch ( NoSuchMethodException e1 ) {
					System.out.println( e );
				}
			}
			c = c.getSuperclass( );
		}
		return null;
	}

	// TODO ! combine with evaluateMember #fix ^2 
	private void evaluate( final Object theObject , final String theName ) {

		Class< ? > c = theObject.getClass( );

		while ( c != null ) {
			try {
				final Field field = c.getDeclaredField( theName );
				field.setAccessible( true );
				try {
					final Object value = field.get( theObject );
					addListener( field , theObject , theName , value );
				} catch ( Exception e ) {}

			} catch ( Exception e ) {}
			c = c.getSuperclass( );
		}
	}

	static private Map toMap( final String s ) {
		/* similar to mapFrom(Object ... args) but with type (Number,String)
		 * sensitivity */
		String[] arr = s.trim( ).split( " " );
		Map m = new LinkedHashMap( );
		if ( arr.length % 2 == 0 ) {
			for ( int i = 0 ; i < arr.length ; i += 2 ) {
				String s1 = arr[ i + 1 ];
				m.put( arr[ i ] , isNumeric( s1 ) ? s1.indexOf( "." ) == -1 ? i( s1 ) : f( s1 ) : s1 );
			}
		}
		return m;
	}

	static public Map toMap( final Object ... args ) {
		Map m = new LinkedHashMap( );
		if ( args.length % 2 == 0 ) {
			for ( int i = 0 ; i < args.length ; i += 2 ) {
				m.put( args[ i ] , args[ i + 1 ] );
			}
		}
		return m;
	}

	static private List toList( final Object o ) {
		return o != null ? ( o instanceof List ) ? ( List ) o : ( o instanceof String ) ? toList( o.toString( ) ) : Collections.emptyList( ) : Collections.emptyList( );
	}

	static private boolean notNull( final Object ... os ) {
		for ( Object o : os ) {
			if ( o == null ) {
				return false;
			}
		}
		return true;
	}

	static private float f( final Object o ) {
		return f( o , Float.MIN_VALUE );
	}

	static private float f( final Object o , final float theDefault ) {
		return ( o instanceof Number ) ? ( ( Number ) o ).floatValue( ) : ( o instanceof String ) ? f( s( o ) ) : theDefault;
	}

	static private float f( final String o ) {
		return f( o , Float.MIN_VALUE );
	}

	static private float f( final String o , final float theDefault ) {
		return isNumeric( o ) ? Float.parseFloat( o ) : theDefault;
	}

	static private boolean isNumeric( final Object o ) {
		return isNumeric( o.toString( ) );
	}

	static private boolean isNumeric( final String str ) {
		return str.matches( "(-|\\+)?\\d+(\\.\\d+)?" );
	}

	static private String s( final Object o ) {
		return ( o != null ) ? o.toString( ) : "";
	}

	static private String s( final Object o , final String theDefault ) {
		return ( o != null ) ? o.toString( ) : theDefault;
	}

	static private String s( final Number o , int theDec ) {
		return ( o != null ) ? String.format( "%." + theDec + "f" , o.floatValue( ) ) : "";
	}

	static private int i( final Object o ) {
		return i( o , Integer.MIN_VALUE );
	}

	static private int i( final Object o , final int theDefault ) {
		return ( o instanceof Number ) ? ( ( Number ) o ).intValue( ) : ( o instanceof String ) ? i( s( o ) ) : theDefault;
	}

	static private int i( final String o ) {
		return i( o , Integer.MIN_VALUE );
	}

	static private int i( final String o , final int theDefault ) {
		return isNumeric( o ) ? Integer.parseInt( o ) : theDefault;
	}

	static private void println( final Object ... strs ) {
		for ( Object str : strs ) {
			System.out.print( str + " " );
		}
		System.out.println( );
	}

	private static class InspectorItem {

		public int draw( PGraphics g , Map m ) {
			return 0;
		}
	}

	private final void init( ) {
		items.put( "default-item" , new InspectorItem( ) {
			public int draw( PGraphics g , Map m ) {
				g.fill( 255 );
				g.textAlign( PApplet.LEFT );
				g.text( s( m.get( "path" ) ) , 0 , spacing );
				g.textAlign( PApplet.RIGHT );
				g.text( format( m.get( "now" ) ) , g.width - 40 , spacing );
				g.fill( 255 , 64 );
				g.rect( 0 , 24 , pg.width - 40 , 1 );
				return spacing;
			}
		} );

		items.put( "default-list" , new InspectorItem( ) {
			public int draw( PGraphics g , Map m ) {
				g.fill( 255 );
				g.pushMatrix( );
				g.text( s( m.get( "path" ) , "" ) , 0 , spacing );
				List l = ( List ) m.get( "now" );
				float min = f( m.get( "min" ) , 0 );
				float max = f( m.get( "max" ) , 100 );
				float scale = f( m.get( "scale" ) , 1 );
				g.translate( 0 , spacing + 10 );
				g.pushMatrix( );
				for ( Object o : l ) {
					g.rect( 0 , max , 1 , -PApplet.constrain( i( o ) * scale , min , max ) );
					g.translate( 2 , 0 );
				}
				g.popMatrix( );
				g.popMatrix( );
				return i( max + spacing + 10 );
			}
		} );

		items.put( "line-graph" , new InspectorItem( ) {
			public int draw( PGraphics g , Map m ) {
				g.fill( 255 );
				g.pushMatrix( );
				g.text( s( m.get( "path" ) , "" ) , 0 , spacing );
				List l = ( List ) m.get( "now" );
				g.pushMatrix( );
				g.translate( 0 , 50 );
				g.noFill( );
				g.stroke( 255 );
				g.strokeWeight( 1.5f );
				g.beginShape( );
				float x = 0;
				float w = g.width - 40;
				float step = w / l.size( );
				for ( Object o : l ) {
					g.vertex( x += step , -PApplet.map( i( o ) , 0 , 100 , 0 , spacing ) );
				}
				g.endShape( );
				g.popMatrix( );
				g.noStroke( );
				g.fill( 255 , 64 );
				g.rect( 0 , 50 , w , 1 );
				g.popMatrix( );
				return 50 + spacing;
			}
		} );

	}
}