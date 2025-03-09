package de.franklisting.fsm.testutil

import de.franklisting.fsm.Fsm
import de.franklisting.fsm.NoEvent
import de.franklisting.fsm.StartEvent
import de.franklisting.fsm.State
import de.franklisting.fsm.Transition
import de.franklisting.fsm.testutil.DiagramGenerator.Companion.toFile
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
) {
    private val mainFsmGenerator = DiagramGenerator(fsm)

    private val childGenerators =
        mainFsmGenerator.states
            .flatMap { it.debugInterface.childDump }
            .distinct()
            .map { DiagramGenerator(it) }

    private fun generateGraph(): Graph {
        val graphs =
            (listOf(mainFsmGenerator.generateGraphWithLabel()) + childGenerators.map { it.generateGraphWithLabel() }).map { it.cluster() }

        return graph().directed().with(graphs)
    }

    fun toSvg(
        fileName: String,
        width: Int = 1000,
    ) {
        toFile(generateGraph(), fileName, width, SVG)
    }

    fun toPng(
        fileName: String,
        width: Int = 1000,
    ) {
        toFile(generateGraph(), fileName, width, PNG)
    }
}

class DiagramGenerator<T>(
    internal val fsm: Fsm<T>,
) {
    internal val states: List<State<T>> = findStates()

    private fun findStates(): List<State<T>> {
        val states = mutableSetOf(fsm.currentState)
        findStates(states, fsm.currentState.debugInterface.transitionDump)
        return states.toList()
    }

    private fun findStates(
        states: MutableSet<State<T>>,
        t: List<Transition<T>>,
    ) {
        t.forEach {
            if (states.add(it.endPoint.state)) {
                findStates(states, it.endPoint.state.debugInterface.transitionDump)
            }
        }
    }

    fun generateGraphWithLabel(): Graph =
        generateGraph()
            .graphAttr()
            .with(
                Label.of(fsm.name).justify(Label.Justification.LEFT),
                Font.name("Arial"),
                Font.size(10),
            )

    private fun generateGraph(): Graph {
        val stateNodes = states.associateWith { it.node }
        val links =
            stateNodes.map { entry ->
                entry.value.link(
                    entry.key.debugInterface.transitionDump
                        .mapNotNull { stateNodes[it.endPoint.state]?.use(it) },
                )
            }

        return graph(fsm.name)
            .directed()
            .with(links)
    }

    fun toSvg(
        fileName: String,
        width: Int = 500,
    ) {
        toFile(generateGraph(), fileName, width, SVG)
    }

    fun toPng(
        fileName: String,
        width: Int = 500,
    ) {
        toFile(generateGraph(), fileName, width, PNG)
    }

    companion object {
        private val <T>State<T>.standardNode: Node
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

        private val <T>State<T>.parentNode: Node
            get() = standardNode.with("peripheries", "2")

        private val <T>State<T>.initialNode: Node
            get() =
                node(id)
                    .with(
                        Style.FILLED,
                        Shape.CIRCLE,
                        Color.BLACK,
                        Label.of(""),
                    ).with(Size.mode(Size.Mode.FIXED).size(0.3, 0.3))

        private val <T>State<T>.finalNode: Node
            get() =
                node(id)
                    .with(
                        Style.FILLED,
                        Shape.DOUBLE_CIRCLE,
                        Color.BLACK,
                        Label.of(""),
                    ).with(Size.mode(Size.Mode.FIXED).size(0.3, 0.3))

        val <T>State<T>.node: Node
            get() =
                when {
                    isInitial -> initialNode
                    isFinal -> finalNode
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
    }

    private val <T>Transition<T>.label
        get() =
            Label.of(
                when (trigger::class) {
                    StartEvent::class, NoEvent::class -> ""
                    else -> trigger.name
                },
            )

    private fun <T> Node.use(transition: Transition<T>): Link = Factory.to(this).with(transition.label, Font.name("Arial"), Font.size(10))
}
