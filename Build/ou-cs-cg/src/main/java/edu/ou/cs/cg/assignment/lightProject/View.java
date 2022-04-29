//******************************************************************************
// Copyright (C) 2016-2022 University of Oklahoma Board of Trustees.
//******************************************************************************
// Last modified: Fri Feb 25 23:46:18 2022 by Chris Weaver
//******************************************************************************
// Major Modification History:
//
// 20160209 [weaver]:	Original file.
// 20190203 [weaver]:	Updated to JOGL 2.3.2 and cleaned up.
// 20190227 [weaver]:	Updated to use model and asynchronous event handling.
// 20220225 [weaver]:	Added point smoothing for Hi-DPI displays.
//
//******************************************************************************
// Notes:
//
//******************************************************************************

package edu.ou.cs.cg.assignment.homework03;

//import java.lang.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.*;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;
import edu.ou.cs.cg.utilities.Utilities;

//******************************************************************************

/**
 * The <CODE>View</CODE> class.<P>
 *
 * @author  Chris Weaver
 * @version %I%, %G%
 */
public final class View
	implements GLEventListener
{
	//**********************************************************************
	// Private Class Members
	//**********************************************************************

	private static final int			DEFAULT_FRAMES_PER_SECOND = 60;
	private static final DecimalFormat	FORMAT = new DecimalFormat("0.000");

	//**********************************************************************
	// Public Class Members
	//**********************************************************************

	public static final GLUT			MYGLUT = new GLUT();
	public static final Random			RANDOM = new Random();

	//**********************************************************************
	// Private Members
	//**********************************************************************

	// State (internal) variables
	private final GLJPanel				canvas;
	private int						w;			// Canvas width
	private int						h;			// Canvas height

	private TextRenderer				renderer;

	private final FPSAnimator			animator;
	private int						counter;	// Frame counter

	private final Model				model;

	private final KeyHandler			keyHandler;
	private final MouseHandler			mouseHandler;
	
	// These are the trace for the lightbeam
	private final Deque<Point2D.Double>	hitPoints1;
	private final Deque<Point2D.Double> hitPoints2;
	
	// The vectors for the lightpoints
	private Point2D.Double vector1 = new Point2D.Double(1.0 / DEFAULT_FRAMES_PER_SECOND, 0.0);
	
	private Point2D.Double vector2 = new Point2D.Double(1.0 / DEFAULT_FRAMES_PER_SECOND, 0.0);
	
	

	//**********************************************************************
	// Constructors and Finalizer
	//**********************************************************************

	public View(GLJPanel canvas)
	{
		this.canvas = canvas;

		// Initialize rendering
		counter = 0;
		canvas.addGLEventListener(this);

		// Initialize model (scene data and parameter manager)
		model = new Model(this);
		
		hitPoints1 = new ArrayDeque<Point2D.Double>();
		hitPoints2 = new ArrayDeque<Point2D.Double>();

		// Initialize controller (interaction handlers)
		keyHandler = new KeyHandler(this, model);
		mouseHandler = new MouseHandler(this, model);

		// Initialize animation
		animator = new FPSAnimator(canvas, DEFAULT_FRAMES_PER_SECOND);
		animator.start();
	}

	//**********************************************************************
	// Getters and Setters
	//**********************************************************************

	public GLJPanel	getCanvas()
	{
		return canvas;
	}

	public int	getWidth()
	{
		return w;
	}

	public int	getHeight()
	{
		return h;
	}
	
	//**********************************************************************
	// Public methods
	//**********************************************************************
	
	// Clears the trace and resets the vector
	public void clearLight()
	{
		hitPoints1.clear();
		vector1.setLocation(1.0 / DEFAULT_FRAMES_PER_SECOND, 0.0);
	}

	//**********************************************************************
	// Override Methods (GLEventListener)
	//**********************************************************************

	public void	init(GLAutoDrawable drawable)
	{
		w = drawable.getSurfaceWidth();
		h = drawable.getSurfaceHeight();

		renderer = new TextRenderer(new Font("Monospaced", Font.PLAIN, 12),
									true, true);

		initPipeline(drawable);
	}

	public void	dispose(GLAutoDrawable drawable)
	{
		renderer = null;
	}

	public void	display(GLAutoDrawable drawable)
	{
		updatePipeline(drawable);

		update(drawable);
		render(drawable);
	}

	public void	reshape(GLAutoDrawable drawable, int x, int y, int w, int h)
	{
		this.w = w;
		this.h = h;
	}

	//**********************************************************************
	// Private Methods (Rendering)
	//**********************************************************************

	private void	update(GLAutoDrawable drawable)
	{
		counter++;									// Advance animation counter
		
		Point2D.Double lp = model.getLightPoint();
		
		updatePointWithReflection(lp);
	}

	private void	render(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		gl.glClear(GL.GL_COLOR_BUFFER_BIT);		// Clear the buffer

		// Draw the scene
		drawMain(gl);								// Draw main content
		drawMode(drawable);						// Draw mode text

		gl.glFlush();								// Finish and display
	}

	//**********************************************************************
	// Private Methods (Pipeline)
	//**********************************************************************

	private void	initPipeline(GLAutoDrawable drawable)
	{
		GL2	gl = drawable.getGL().getGL2();

		gl.glClearColor(0.306f, 0.306f, 0.306f, 0.0f);	// Black background

		// Make points easier to see on Hi-DPI displays
		gl.glEnable(GL2.GL_POINT_SMOOTH);	// Turn on point anti-aliasing
	}

	private void	updatePipeline(GLAutoDrawable drawable)
	{
		GL2			gl = drawable.getGL().getGL2();
		GLU			glu = GLU.createGLU();
		Point2D.Double	origin = model.getOrigin();

		float			xmin = (float)(origin.x - 1.0);
		float			xmax = (float)(origin.x + 1.0);
		float			ymin = (float)(origin.y - 1.0);
		float			ymax = (float)(origin.y + 1.0);

		gl.glMatrixMode(GL2.GL_PROJECTION);		// Prepare for matrix xform
		gl.glLoadIdentity();						// Set to identity matrix
		glu.gluOrtho2D(0, 1280, 0, 720);	// 2D translate and scale
		//glu.gluOrtho2D(xmin, xmax, ymin, ymax);	// 2D translate and scale
	}

	//**********************************************************************
	// Private Methods (Scene)
	//**********************************************************************

	private void	drawMode(GLAutoDrawable drawable)
	{
		GL2		gl = drawable.getGL().getGL2();
		double[]	p = Utilities.mapViewToScene(gl, 0.5 * w, 0.5 * h, 0.0);
		double[]	q = Utilities.mapSceneToView(gl, 0.0, 0.0, 0.0);
		String		svc = ("View center in scene: [" + FORMAT.format(p[0]) +
						   " , " + FORMAT.format(p[1]) + "]");
		String		sso = ("Scene origin in view: [" + FORMAT.format(q[0]) +
						   " , " + FORMAT.format(q[1]) + "]");

		renderer.beginRendering(w, h);

		// Draw all text in yellow
		renderer.setColor(1.0f, 1.0f, 0.0f, 1.0f);

		Point2D.Double	cursor = model.getCursor();

		if (cursor != null)
		{
			String		sx = FORMAT.format(new Double(cursor.x));
			String		sy = FORMAT.format(new Double(cursor.y));
			String		s = "Pointer at (" + sx + "," + sy + ")";

			renderer.draw(s, 2, 2);
		}
		else
		{
			renderer.draw("No Pointer", 2, 2);
		}

		renderer.draw(svc, 2, 16);
		renderer.draw(sso, 2, 30);

		renderer.endRendering();
	}

	private void	drawMain(GL2 gl)
	{
		drawBounds(gl);							// Unit bounding box
		drawAxes(gl);								// X and Y axes
		drawCursor(gl);							// Crosshairs at mouse point
		drawPolyline(gl);							// Draw the user's sketch
		
		// Light project methods
		drawLight(gl);			// Draw the light beam
		drawLightBox(gl);		// Draw the lightbox
		drawMirrors(gl);		// Draw the mirrors
		drawPrisms(gl);			// Draw the prisms
		drawConvex(gl);			// Draw convex lenses
		drawConcave(gl);		// Draw concave lenses
		
		// Debugging method that draws the lightpoint
		drawObject(gl);
	}
	
	// Debugging method to make the location of the lightpoint known
	private void	drawObject(GL2 gl)
	{
		Point2D.Double	object = model.getLightPoint();

		gl.glColor3f(1.0f, 0.0f, 0.0f);

		gl.glBegin(GL.GL_POINTS);

		gl.glVertex2d(object.x, object.y);

		gl.glEnd();
	}

	// Not used
	private void	drawBounds(GL2 gl)
	{
		gl.glColor3f(0.1f, 0.1f, 0.1f);
		gl.glBegin(GL.GL_LINE_LOOP);

		gl.glVertex2d(1.0, 1.0);
		gl.glVertex2d(-1.0, 1.0);
		gl.glVertex2d(-1.0, -1.0);
		gl.glVertex2d(1.0, -1.0);

		gl.glEnd();
	}

	// Not used
	private void	drawAxes(GL2 gl)
	{
		gl.glBegin(GL.GL_LINES);

		gl.glColor3f(0.25f, 0.25f, 0.25f);
		gl.glVertex2d(-10.0, 0.0);
		gl.glVertex2d(10.0, 0.0);

		gl.glVertex2d(0.0, -10.0);
		gl.glVertex2d(0.0, 10.0);

		gl.glEnd();
	}

	// Not used
	private void	drawCursor(GL2 gl)
	{
		Point2D.Double	cursor = model.getCursor();

		if (cursor == null)
			return;

		gl.glBegin(GL.GL_LINE_LOOP);
		gl.glColor3f(0.5f, 0.5f, 0.5f);

		for (int i=0; i<32; i++)
		{
			double	theta = (2.0 * Math.PI) * (i / 32.0);

			gl.glVertex2d(cursor.x + 0.05 * Math.cos(theta),
						  cursor.y + 0.05 * Math.sin(theta));
		}

		gl.glEnd();
	}

	// Not used
	private void	drawPolyline(GL2 gl)
	{
		java.util.List<Point2D.Double>	points = model.getPolyline();

		gl.glColor3f(1.0f, 0.0f, 0.0f);

		for (Point2D.Double p : points)
		{
			gl.glBegin(GL2.GL_POLYGON);

			gl.glVertex2d(p.x - 0.05, p.y - 0.05);
			gl.glVertex2d(p.x - 0.05, p.y + 0.05);
			gl.glVertex2d(p.x + 0.05, p.y + 0.05);
			gl.glVertex2d(p.x + 0.05, p.y - 0.05);

			gl.glEnd();
		}

		if (model.getColorful())		// Show the psychedelic version...
		{
			float	a = 0.0f;
			float	delta = 360.0f / (float)points.size();

			gl.glColor3f(1.0f, 1.0f, 0.0f);
			gl.glBegin(GL.GL_TRIANGLE_FAN);
			gl.glVertex2d(0.0, 0.0);

			for (Point2D.Double p : points)
			{
				Color	c = new Color(Color.HSBtoRGB(a, 1.0f, 1.0f));
				float[]	rgb = c.getRGBColorComponents(null);

				gl.glColor3f(rgb[0], rgb[1], rgb[2]);
				gl.glVertex2d(p.x, p.y);

				a += delta;
			}

			gl.glEnd();
		}
		else							// ...or the simple version.
		{
			gl.glColor3f(1.0f, 1.0f, 0.0f);
			gl.glBegin(GL.GL_LINE_STRIP);

			for (Point2D.Double p : points)
				gl.glVertex2d(p.x, p.y);

			gl.glEnd();
		}
	}
	
	// Draw the lightbox
	private void drawLightBox(GL2 gl)
	{
		Model.LightBox lightbox = model.getLightBox();
		
		setColor(gl, 93, 201, 244);		// Cyan
		
		gl.glBegin(GL2.GL_POLYGON);
		
		gl.glVertex2d(lightbox.getBl().x, lightbox.getBl().y);
		gl.glVertex2d(lightbox.getBr().x, lightbox.getBr().y);
		gl.glVertex2d(lightbox.getTr().x, lightbox.getTr().y);
		gl.glVertex2d(lightbox.getTl().x, lightbox.getTl().y);
		
		gl.glEnd();
	}
	
	// Draw the lightbeam
	private void drawLight(GL2 gl)
	{
		if(!model.getLight()) {
			return;
		}
		
		gl.glColor3f(1.0f, 1.0f, 1.0f);
		
		gl.glBegin(GL.GL_LINE_STRIP);
		
		for (Point2D.Double hp : hitPoints1)
		{
			gl.glVertex2d(hp.x, hp.y);
		}
		
		gl.glEnd();
	}
	
	// Draw the mirrors
	private void drawMirrors(GL2 gl)
	{
		java.util.List<Model.Mirror> mirrors = model.getMirrors();
		
		setColor(gl, 199, 199, 199);	// Light gray
		
		for (Model.Mirror m : mirrors)
		{
			gl.glBegin(GL2.GL_POLYGON);

			gl.glVertex2d(m.getBl().x, m.getBl().y);
			gl.glVertex2d(m.getBr().x, m.getBr().y);
			gl.glVertex2d(m.getTr().x, m.getTr().y);
			gl.glVertex2d(m.getTl().x, m.getTl().y);

			gl.glEnd();
		}
	}
	
	// Draw the prisms
	private void drawPrisms(GL2 gl)
	{
		java.util.List<Model.Prism> prisms = model.getPrisms();
		
		setColor(gl, 199, 199, 199);	// Light gray
		
		for (Model.Prism p : prisms)
		{
			gl.glBegin(GL2.GL_POLYGON);
			
			gl.glVertex2d(p.getBl().x, p.getBl().y);
			gl.glVertex2d(p.getBr().x, p.getBr().y);
			gl.glVertex2d(p.getT().x, p.getT().y);
			
			gl.glEnd();
		}
	}
	
	// Draw the convex lenses
	private void drawConvex(GL2 gl)
	{
		java.util.List<Model.Lense> convexLenses = model.getConvexLenses();
		
		setColor(gl, 199, 199, 199);	// Light gray
		
		int i;
		double[] rCurveX;
		double[] rCurveY;
		double[] lCurveX;
		double[] lCurveY;
		
		// Calculate Bezier curves for each of the lenses at some point
		// Determine where the control points will be
		
		for (Model.Lense c : convexLenses)
		{
			rCurveX = c.getRCurveX();
			rCurveY = c.getRCurveY();
			lCurveX = c.getLCurveX();
			lCurveY = c.getLCurveY();
			
			gl.glBegin(GL2.GL_POLYGON);
		
			gl.glVertex2d(c.getBl().x, c.getBl().y);
			gl.glVertex2d(c.getBr().x, c.getBr().y);
			
			for(i = 0; i < 11; i++)
			{
				gl.glVertex2d(rCurveX[i], rCurveY[i]);
			}
			
			gl.glVertex2d(c.getTr().x, c.getTr().y);
			gl.glVertex2d(c.getTl().x, c.getTl().y);
			
			for(i = 0; i < 11; i++)
			{
				gl.glVertex2d(lCurveX[i], lCurveY[i]);
			}
			
			gl.glEnd();
		}
	}
	
	// Draw the concave lenses
	private void drawConcave(GL2 gl)
	{
		java.util.List<Model.Lense> concaveLenses = model.getConcaveLenses();
		
		int i;
		double[] rCurveX;
		double[] rCurveY;
		double[] lCurveX;
		double[] lCurveY;
		
		setColor(gl, 199, 199, 199);	// Light gray
		
		// Calculate Bezier curves for each of the lenses at some point
		// The control point will be the center point
		
		for (Model.Lense c : concaveLenses)
		{
			rCurveX = c.getRCurveX();
			rCurveY = c.getRCurveY();
			lCurveX = c.getLCurveX();
			lCurveY = c.getLCurveY();
			
			gl.glBegin(GL2.GL_TRIANGLE_FAN);
			
			gl.glVertex2d(c.getCenter().x, c.getCenter().y);
			
			gl.glVertex2d(c.getBl().x, c.getBl().y);
			gl.glVertex2d(c.getBr().x, c.getBr().y);
			
			for(i = 0; i < 11; i++)
			{
				gl.glVertex2d(rCurveX[i], rCurveY[i]);
			}
			
			gl.glVertex2d(c.getTr().x, c.getTr().y);
			gl.glVertex2d(c.getTl().x, c.getTl().y);
			
			for(i = 0; i < 11; i++)
			{
				gl.glVertex2d(lCurveX[i], lCurveY[i]);
				//System.out.println(lCurveX[i]);
			}
			
			gl.glEnd();
		}
	}
	
	// Update the location of the light point
	private void updatePointWithReflection(Point2D.Double lp)
	{
		// Check if a lightbox has been set
		if((lp.x == 0.0 && lp.y == 0.0) || !model.getLight())
		{
			return;
		}
		
		// Travel factor and factored direction vector
		double factor = w * 0.1;
		double ddx = vector1.x * factor;
		double ddy = vector1.y * factor;
		
		// All the object lists, new object class should be able to get rid of these
		ArrayDeque<Point2D.Double> nodes = new ArrayDeque<Point2D.Double>(model.getNodes());
		java.util.List<Model.Lense> convexLenses = model.getConvexLenses();
		java.util.List<Model.Lense> concaveLenses = model.getConcaveLenses();
		java.util.List<Model.Prism> prisms = model.getPrisms();
		java.util.List<Model.Mirror> mirrors = model.getMirrors();
		Model.LightBox lightbox = model.getLightBox();
		
		// Method variables
		int number = nodes.size();
		int i;
		String type = "NA";
		String hitType = "NA";
		int index;
		
		pointCalc: while (true)
		{
			// Calculate which side the point will reach first. These variables
			// store that side's vertices and the parametric time to hit it.
			Point2D.Double		pp1 = new Point2D.Double();
			Point2D.Double		pp2 = new Point2D.Double();
			double				tmin = Double.MAX_VALUE;
			
			// Temp used to store previous tmin value
			double 				temp;
			
			/*
			if(hitPoints1.isEmpty())
			{
				hitPoints1.add(new Point2D.Double(lp.x, lp.y));
			}
			lp.x += ddx;
			lp.y += ddy;
			hitPoints1.add(new Point2D.Double(lp.x, lp.y));
			break;
			*/
			
			// Adds a point to the trace if there are none
			if(hitPoints1.isEmpty())
			{
				hitPoints1.add(new Point2D.Double(lp.x, lp.y));
			}
			
			// Get the tmins of all objects, should be simpler with new method
			for(i = 0; i < number; i++)
			{
				Point2D.Double node = nodes.peekFirst();
				type = findType(node);
				index = getIndex(type, node);
				temp = tmin;
				switch(type) 
				{
					case "Convex":
						tmin = getTMin(convexLenses.get(index).getBl(), convexLenses.get(index).getBr(),
								ddx, ddy, pp1, pp2, tmin, lp);
						tmin = getTMin(convexLenses.get(index).getBr(), convexLenses.get(index).getTr(),
								ddx, ddy, pp1, pp2, tmin, lp);
						tmin = getTMin(convexLenses.get(index).getTr(), convexLenses.get(index).getTl(),
								ddx, ddy, pp1, pp2, tmin, lp);
						tmin = getTMin(convexLenses.get(index).getTl(), convexLenses.get(index).getBl(),
								ddx, ddy, pp1, pp2, tmin, lp);
						break;
					case "Concave":
						tmin = getTMin(concaveLenses.get(index).getBl(), concaveLenses.get(index).getBr(),
								ddx, ddy, pp1, pp2, tmin, lp);
						tmin = getTMin(concaveLenses.get(index).getBr(), concaveLenses.get(index).getTr(),
								ddx, ddy, pp1, pp2, tmin, lp);
						tmin = getTMin(concaveLenses.get(index).getTr(), concaveLenses.get(index).getTl(),
								ddx, ddy, pp1, pp2, tmin, lp);
						tmin = getTMin(concaveLenses.get(index).getTl(), concaveLenses.get(index).getBl(),
								ddx, ddy, pp1, pp2, tmin, lp);
						break;
					case "Prism":
						tmin = getTMin(prisms.get(index).getBl(), prisms.get(index).getBr(),
								ddx, ddy, pp1, pp2, tmin, lp);
						tmin = getTMin(prisms.get(index).getBr(), prisms.get(index).getT(), 
								ddx, ddy, pp1, pp2, tmin, lp);
						tmin = getTMin(prisms.get(index).getT(), prisms.get(index).getBl(), 
								ddx, ddy, pp1, pp2, tmin, lp);
						break;
					case "Mirror":
						tmin = getTMin(mirrors.get(index).getBl(), mirrors.get(index).getBr(), 
								ddx, ddy, pp1, pp2, tmin, lp);
						tmin = getTMin(mirrors.get(index).getBr(), mirrors.get(index).getTr(), 
								ddx, ddy, pp1, pp2, tmin, lp);
						tmin = getTMin(mirrors.get(index).getTr(), mirrors.get(index).getTl(), 
								ddx, ddy, pp1, pp2, tmin, lp);
						tmin = getTMin(mirrors.get(index).getTl(), mirrors.get(index).getBl(),
								ddx, ddy, pp1, pp2, tmin, lp);
						break;
					case "Lightbox":
						tmin = getTMin(lightbox.getBl(), lightbox.getBr(), ddx, ddy, pp1, pp2, tmin, lp);
						tmin = getTMin(lightbox.getBr(), lightbox.getTr(), ddx, ddy, pp1, pp2, tmin, lp);
						tmin = getTMin(lightbox.getTr(), lightbox.getTl(), ddx, ddy, pp1, pp2, tmin, lp);
						tmin = getTMin(lightbox.getTl(), lightbox.getBl(), ddx, ddy, pp1, pp2, tmin, lp);
						break;
				}
				
				// Sets the hit type to the lowest tmin object
				if(tmin < temp)
				{
					hitType = new String(type);
				}
				nodes.offerLast(nodes.pollFirst());
			}
			
			System.out.printf("%f \n", tmin);

			// Increment normally if greater than the factor (I believe this is right)
			if(tmin > factor)
			{
				lp.x += ddx;
				lp.y += ddy;
				hitPoints1.add(new Point2D.Double(lp.x, lp.y));
				
				break;
			}
			else
			{
				System.out.println(hitType);
				switch(hitType)
				{
					case "Mirror":
						// If it is under 1.0, there will be at least one reflection.
						// Translate the point to the reflection point along the side.
						//lp.x += ddx * tmin;
						//lp.y += ddy * tmin;
						lp.x += ddx * (tmin / 100);
						lp.y += ddy * (tmin / 100);
						hitPoints1.offerLast(new Point2D.Double(lp.x, lp.y));

						// Calculate the CW (outward-pointing) perp vector for the side.
						double		vdx = pp2.x - pp1.x;	// Calc side vector
						double		vdy = pp2.y - pp1.y;	// from p1 to p2
						double		ndx = vdy;			// Calc perp vector:
						double		ndy = -vdx;				// negate x and swap

						// Need a NORMALIZED perp vector for the reflection calculation.
						double		nn = Math.sqrt(ndx * ndx + ndy * ndy);

						ndx = ndx / nn;	// Divide each coordinate by the length to
						ndy = ndy / nn;	// make a UNIT vector normal to the side.

						// Calculate v_reflected. See pages 148-149 and the slide on
						// "Reflecting Trajectories". (Note: P and v on the slide are
						// named q and dd here.)
						double		dot = dot(vector1.x * 2, vector1.y * 2, 0.0, ndx, ndy, 0.0);
						double		vreflectedx = ddx - 2.0 * dot * ndx;
						double		vreflectedy = ddy - 2.0 * dot * ndy;

						// Reflect the update vector, and reduce it to compensate for
						// the distance the point moved to reach the side.
						ddx = vreflectedx * (factor - tmin);
						ddy = vreflectedy * (factor - tmin);

						// Also reflect the reference vector. It will change direction
						// but remain the same length.
						double		dot2 = dot(vector1.x, vector1.y, 0.0, ndx, ndy, 0.0);

						vector1.x -= 2.0 * dot2 * ndx;
						vector1.y -= 2.0 * dot2 * ndy;
						break;
					case "Lightbox":
						// Move lightpoint to the edge of the lightbox and stop it
						lp.x += ddx * (tmin / 100);
						lp.y += ddy * (tmin / 100);
						vector1.x = 0.0;
						vector1.y = 0.0;
						hitPoints1.offerLast(new Point2D.Double(lp.x, lp.y));
						break pointCalc;
				}
			}
		}
	}
	
	//**********************************************************************
	// Private Methods (Utility Functions)
	//**********************************************************************
	
	// Finds the object type of the current node, part of old method
	private String findType(Point2D.Double node)
	{
		java.util.List<Model.Lense> convexLenses = model.getConvexLenses();
		java.util.List<Model.Lense> concaveLenses = model.getConcaveLenses();
		java.util.List<Model.Prism> prisms = model.getPrisms();
		java.util.List<Model.Mirror> mirrors = model.getMirrors();
		Model.LightBox lightbox = model.getLightBox();
		int i;
		
		if(!convexLenses.isEmpty()) 
		{
			for(i = 0; i < convexLenses.size(); i++)
			{
				if(convexLenses.get(i).getCenter().equals(node))
				{
					return "Convex";
				}
			}
		}
		
		if(!concaveLenses.isEmpty())
		{
			for(i = 0; i < concaveLenses.size(); i++)
			{
				if(concaveLenses.get(i).getCenter().equals(node))
				{
					return "Concave";
				}
			}
		}
		
		if(!prisms.isEmpty())
		{
			for(i = 0; i < prisms.size(); i++)
			{
				if(prisms.get(i).getCenter().equals(node))
				{
					return "Prism";
				}
			}
		}
		
		if(!mirrors.isEmpty())
		{
			for(i = 0; i < mirrors.size(); i++)
			{
				if(mirrors.get(i).getCenter().equals(node))
				{
					return "Mirror";
				}
			}
		}
		
		if(lightbox.getCenter().equals(node))
		{
			return "Lightbox";
		}
		
		return "NA";
	}
	
	// Finds the index of the node, part of old method
	private int getIndex(String type, Point2D.Double node)
	{
		java.util.List<Model.Lense> convexLenses = model.getConvexLenses();
		java.util.List<Model.Lense> concaveLenses = model.getConcaveLenses();
		java.util.List<Model.Prism> prisms = model.getPrisms();
		java.util.List<Model.Mirror> mirrors = model.getMirrors();
		Model.LightBox lightbox = model.getLightBox();
		int i;
		switch(type) 
		{
			case "Convex":
				for(i = 0; i < convexLenses.size(); i++)
				{
					if(convexLenses.get(i).getCenter().equals(node))
					{
						return i;
					}
				}
				break;
			case "Concave":
				for(i = 0; i < concaveLenses.size(); i++)
				{
					if(concaveLenses.get(i).getCenter().equals(node))
					{
						return i;
					}
				}
				break;
			case "Prism":
				for(i = 0; i < prisms.size(); i++)
				{
					if(prisms.get(i).getCenter().equals(node))
					{
						return i;
					}
				}
				break;
			case "Mirror":
				for(i = 0; i < mirrors.size(); i++)
				{
					if(mirrors.get(i).getCenter().equals(node))
					{
						return i;
					}
				}
				break;
			case "Lightbox":
				return 0;
		}
		return -1;
	}
	
	// Gets the tmin of object within the path of the lightpoint
	private double getTMin(Point2D.Double p1, Point2D.Double p2, double ddx, double ddy, 
			Point2D.Double pp1, Point2D.Double pp2, double tmin, Point2D.Double lp)
	{
		// Calculate the CW (outward-pointing) perp vector for the pair.
		double			vdx = p2.x - p1.x;		// Calc side vector
		double			vdy = p2.y - p1.y;		// from p1 to p2
		double			ndx = vdy;			// Calc perp vector:
		double			ndy = -vdx;				// negate y and swap
		
		// Check if point is inbetween
		double vdn = Math.sqrt(vdx * vdx + vdy * vdy);
		double nvdx = vdx / vdn;
		double nvdy = vdy / vdn;
		double pdx = lp.x + ddx - p1.x;
		double pdy = lp.y + ddy - p1.y;
		double dd = dot(nvdx, nvdy, 0.0, pdx, pdy, 0.0);
		if(!(dd >= 0 && dd <= vdn))
		{
			return tmin;
		}

		// See page 175 and the slide on "Intersection of a Line through
		// a Line". (Note: R and v on the slide are named q and w here.)
		double			wdx = p1.x - lp.x;		// Calc test vector
		double			wdy = p1.y - lp.y;		// from q to p1

		// Calculate the top part of the t_hit equation.
		double			dnw = dot(ndx, ndy, 0.0, wdx, wdy, 0.0);

		// Check if q is strictly on the inside of the polygon. The dot
		// product will be 0 if q is on a side, or slightly positive if
		// it is beyond it (which can happen due to roundoff error). See
		// Figure 4.37 and the dot products below it on page 176.
		if (dnw < 0.0)
		{
			// Calculate the bottom part of the t_hit equation.
			double	dnv = dot(ndx, ndy, 0.0, vector1.x * 2, vector1.y * 2, 0.0);

			// If the dot product is zero, the direction of motion is
			// parallel to the side. Disqualify it as a hit candidate
			// (even if the point is exactly ON the side).
			double	thit = ((dnv != 0.0) ? (dnw / dnv) : 0.0);

			// Remember the side with the smallest positive t_hit.
			// It's the side that the point will reach first.
			if ((0.0 < thit) && (thit < tmin))
			{
				pp1.setLocation(p1.x, p1.y);
				pp2.setLocation(p2.x, p2.y);
				return thit;
			}
		}
		return tmin;
	}
	
	// Dot product method
	private double dot(double vx, double vy, double vz, double wx, double wy, double wz)
	{
		return (vx * wx + vy * wy + vz * wz);
	}
	
	// Sets color, normalizing r, g, b, a values from max 255 to 1.0.
	private void	setColor(GL2 gl, int r, int g, int b, int a)
	{
		gl.glColor4f(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f);
	}

	// Sets fully opaque color, normalizing r, g, b values from max 255 to 1.0.
	private void	setColor(GL2 gl, int r, int g, int b)
	{
		setColor(gl, r, g, b, 255);
	}
}

//******************************************************************************
