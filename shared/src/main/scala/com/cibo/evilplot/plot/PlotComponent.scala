package com.cibo.evilplot.plot

import com.cibo.evilplot.colors.{Color, DefaultColors}
import com.cibo.evilplot.geometry._

// A component that is aligned with the data of a plot.
private[plot] abstract class PlotComponent {

  // The position of this component.
  val position: PlotComponent.Position

  // Get the minimum size of this component.
  def size[T](plot: Plot[T]): Extent = Extent(0, 0)

  // Render the component.
  def render[T](plot: Plot[T], extent: Extent): Drawable
}

object PlotComponent {
  private[plot] sealed trait Position
  private[plot] case object Top extends Position
  private[plot] case object Bottom extends Position
  private[plot] case object Left extends Position
  private[plot] case object Right extends Position
  private[plot] case object Overlay extends Position
  private[plot] case object Background extends Position

  private[plot] case class OverlayPlotComponent(
    f: (Plot[_], Extent) => Drawable,
    x: Double,
    y: Double
  ) extends PlotComponent {
    require(x >= 0.0 && x <= 1.0, s"x must be between 0.0 and 1.0, got $x")
    require(y >= 0.0 && y <= 1.0, s"y must be between 0.0 and 1.0, got $y")
    val position: Position = Overlay
    def render[T](plot: Plot[T], extent: Extent): Drawable = {
      val drawable = f(plot, extent)
      val xoffset = (extent.width - drawable.extent.width) * x
      val yoffset = (extent.height - drawable.extent.height) * y
      Translate(drawable, x = xoffset, y = yoffset)
    }
  }

  private[plot] case class BackgroundPlotComponent(
    f: (Plot[_], Extent) => Drawable
  ) extends PlotComponent {
    val position: Position = Background
    def render[T](plot: Plot[T], extent: Extent): Drawable = f(plot, extent)
  }

  private[plot] case class PadPlotComponent(
    position: Position,
    pad: Double
  ) extends PlotComponent {
    override def size[T](plot: Plot[T]): Extent = Extent(pad, pad)
    def render[T](plot: Plot[T], extent: Extent): Drawable = EmptyDrawable(size(plot))
  }

  trait AnnotationImplicits[T] {
    protected val plot: Plot[T]

    /** Add an annotation to the plot.
      * @param f A function to create the drawable to render.
      * @param x The X coordinate to plot the drawable (between 0 to 1).
      * @param y The Y coordinate to plot the drawable (between 0 and 1).
      * @return The updated plot.
      */
    def annotate(f: (Plot[_], Extent) => Drawable, x: Double, y: Double): Plot[T] = {
      plot :+ OverlayPlotComponent(f, x, y)
    }

    /** Add a text annotation to the plot.
      * @param msg The annotation.
      * @param x The X coordinate.
      * @param y The Y coordinate.
      * @return
      */
    def annotate(msg: String, x: Double = 1.0, y: Double = 0.5): Plot[T] =
      annotate((_, _) => msg.split('\n').map(Text(_)).reduce(above), x, y)

    /** Set the background (this will replace any existing background).
      * @param f Function to render the background.
      */
    def background(f: (Plot[_], Extent) => Drawable): Plot[T] = {
      // Place the background on the bottom so that it goes under grid lines, etc.
      val bg = BackgroundPlotComponent(f)
      bg +: plot.copy(components = plot.components.filterNot(_.isInstanceOf[BackgroundPlotComponent]))
    }

    /** Add a solid background.
      * @param color The background color
      */
    def background(color: Color = DefaultColors.backgroundColor): Plot[T] =
      background((_, e) => Rect(e).filled(color))

    def padTop(size: Double): Plot[T] = plot :+ PadPlotComponent(Top, size)
    def padBottom(size: Double): Plot[T] = plot :+ PadPlotComponent(Bottom, size)
    def padLeft(size: Double): Plot[T] = plot :+ PadPlotComponent(Left, size)
    def padRight(size: Double): Plot[T] = plot :+ PadPlotComponent(Right, size)
  }
}
