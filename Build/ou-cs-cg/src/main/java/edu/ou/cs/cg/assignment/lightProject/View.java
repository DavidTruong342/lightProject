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
		drawLightBox(gl);
		drawMirrors(gl);
		drawPrisms(gl);
		drawConvex(gl);
		drawConcave(gl);
	}

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
	
	//**********************************************************************
	// Private Methods (Utility Functions)
	//**********************************************************************
	
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
