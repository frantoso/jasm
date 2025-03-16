package de.franklisting.fsm.testutil

import de.franklisting.fsm.FinalState
import de.franklisting.fsm.Fsm
import de.franklisting.fsm.IState
import de.franklisting.fsm.InitialState
import de.franklisting.fsm.NoEvent
import de.franklisting.fsm.StartEvent
import de.franklisting.fsm.StateContainerBase
import de.franklisting.fsm.Transition
import guru.nidi.graphviz.attribute.Color
import guru.nidi.graphviz.attribute.Font
import guru.nidi.graphviz.attribute.Label
import guru.nidi.graphviz.attribute.Shape
import guru.nidi.graphviz.attribute.Size
import guru.nidi.graphviz.attribute.Style
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Format.PNG
import guru.nidi.graphviz.engine.Format.SVG
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.model.Factory
import guru.nidi.graphviz.model.Factory.graph
import guru.nidi.graphviz.model.Factory.node
import guru.nidi.graphviz.model.Graph
import guru.nidi.graphviz.model.Link
import guru.nidi.graphviz.model.Node
import java.io.File

// https://github.com/nidi3/graphviz-java
class MultipleDiagramGenerator<T>(
    fsm: Fsm<T>,
    private val level: Int = 0,
) : DiagramGenerator<T>(fsm) {
    private val childGenerators =
        states
            .flatMap { it.debugInterface.childDump }
            .distinct()
            .map { MultipleDiagramGenerator(it, level + 1) }

    private val graphs: List<Graph> = listOf(graphWithLabel) + childGenerators.flatMap { it.graphs }

    override val fsmGraph: Graph =
        graphs
            .map { it.cluster() }
            .let { graph().directed().with(it) }
}

open class DiagramGenerator<T>(
    internal val fsm: Fsm<T>,
) {
    internal val states: List<StateContainerBase<T, out IState>> =
        listOf(fsm.debugInterface.initialState) + fsm.debugInterface.stateDump

    private val stateNodes = states.map { it to it.node }

    private val links =
        stateNodes.map { entry ->
            entry.second.link(
                entry.first.debugInterface.transitionDump
                    .mapNotNull { transition ->
                        stateNodes.firstOrNull { transition.endPoint.state == it.first.state }?.second?.use(
                            transition,
                        )
                    },
            )
        }

    private val rawGraph = graph(fsm.name).directed().with(links)

    internal val graphWithLabel =
        rawGraph
            .graphAttr()
            .with(
                Label.of(fsm.name).justify(Label.Justification.LEFT),
                Font.name("Arial"),
                Font.size(10),
            )

    protected open val fsmGraph: Graph = rawGraph

    fun toSvg(
        fileName: String,
        width: Int = 500,
    ) = toFile(fsmGraph, fileName, width, SVG)

    fun toPng(
        fileName: String,
        width: Int = 500,
    ) = toFile(fsmGraph, fileName, width, PNG)

    companion object {
        private val <T>StateContainerBase<T, out IState>.standardNode: Node
            get() =
                node(id)
                    .with(
                        "style",
                        "rounded,filled",
                    ).with(
                        Shape.BOX,
                        Color.BLACK,
                        Color.LIGHTSKYBLUE.fill(),
                        Label.of(name),
                    ).with(Font.name("Arial"))

        private val <T>StateContainerBase<T, out IState>.parentNode: Node
            get() = standardNode.with("peripheries", "2")

        private val <T>StateContainerBase<T, out IState>.initialNode: Node
            get() =
                node(id)
                    .with(
                        Style.FILLED,
                        Shape.CIRCLE,
                        Color.BLACK,
                        Label.of(""),
                    ).with(Size.mode(Size.Mode.FIXED).size(0.3, 0.3))

        private val <T>StateContainerBase<T, out IState>.finalNode: Node
            get() =
                node(id)
                    .with(
                        Style.FILLED,
                        Shape.DOUBLE_CIRCLE,
                        Color.BLACK,
                        Label.of(""),
                    ).with(Size.mode(Size.Mode.FIXED).size(0.3, 0.3))

        val <T>StateContainerBase<T, out IState>.node: Node
            get() =
                when {
                    state is InitialState -> initialNode
                    state is FinalState -> finalNode
                    hasChildren -> parentNode
                    else -> standardNode
                }

        fun toFile(
            graph: Graph,
            fileName: String,
            width: Int,
            format: Format,
        ) {
            Graphviz
                .fromGraph(graph)
                .width(width)
                .render(format)
                .toFile(File(fileName))
        }

        private val <T>Transition<T>.label
            get() =
                Label.of(
                    when (trigger::class) {
                        StartEvent::class, NoEvent::class -> ""
                        else -> trigger.name
                    },
                )

        private fun <T> Node.use(transition: Transition<T>): Link =
            Factory.to(this).with(transition.label, Font.name("Arial"), Font.size(10))
    }
}
