package io.github.frantoso.jasm.testutil

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
import io.github.frantoso.jasm.FinalState
import io.github.frantoso.jasm.Fsm
import io.github.frantoso.jasm.History
import io.github.frantoso.jasm.IState
import io.github.frantoso.jasm.ITransition
import io.github.frantoso.jasm.InitialState
import io.github.frantoso.jasm.NoEvent
import io.github.frantoso.jasm.StartEvent
import io.github.frantoso.jasm.StateContainerBase
import io.github.frantoso.jasm.TransitionEndPoint
import java.io.File

fun List<StateContainerBase<out IState>>.complete(infos: List<StateInfo>): List<StateContainerInfo> {
    val stateContainerInfoMap = this.associate { it.state to StateContainerInfo(it) }.toMutableMap()
    infos.forEach {
        val info = stateContainerInfoMap[it.state]!!
        stateContainerInfoMap[it.state] = info.add(it)
    }
    return stateContainerInfoMap.values.toList()
}

class StateInfo(
    endPoint: TransitionEndPoint,
) {
    val state: IState = endPoint.state
    val hasHistory = endPoint.history.isHistory
    val hasDeepHistory = endPoint.history.isDeepHistory
}

class StateContainerInfo(
    val container: StateContainerBase<out IState>,
    val hasHistory: Boolean = false,
    val hasDeepHistory: Boolean = false,
) {
    fun add(stateInfo: StateInfo): StateContainerInfo =
        StateContainerInfo(
            container,
            hasHistory || stateInfo.hasHistory,
            hasDeepHistory || stateInfo.hasDeepHistory,
        )
}

// https://github.com/nidi3/graphviz-java
class MultipleDiagramGenerator(
    fsm: Fsm,
    private val level: Int = 0,
) : DiagramGenerator(fsm) {
    private val childGenerators =
        stateContainers
            .flatMap { it.debugInterface.childDump }
            .distinct()
            .map { MultipleDiagramGenerator(it, level + 1) }

    private val graphs: List<Graph> = listOf(graphWithLabel) + childGenerators.flatMap { it.graphs }

    override val fsmGraph: Graph =
        graphs
            .map { it.cluster() }
            .let { graph().directed().with(it) }
}

open class DiagramGenerator(
    fsm: Fsm,
) {
    internal val stateContainers: List<StateContainerBase<out IState>> =
        listOf(fsm.debugInterface.initialState) + fsm.debugInterface.stateDump

    private val transitions = stateContainers.flatMap { it.transitions }

    private val stateContainerInfos = stateContainers.complete(transitions.map { StateInfo(it.endPoint) })

    private val stateNodes = stateContainerInfos.map { it to it.node }

    private val linksX =
        stateNodes.map { entry ->
            entry.second.link(
                entry.first.container.debugInterface.transitionDump
                    .mapNotNull { transition ->
                        stateNodes.firstOrNull { transition.endPoint.state == it.first.container.state }?.second?.use(
                            transition,
                        )
                    },
            )
        }

    private val rawGraph = graph(fsm.name).directed().with(linksX)

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
        private val StateContainerBase<out IState>.standardNodeBase: Node
            get() =
                node(id)
                    .with(
                        "style",
                        "rounded,filled",
                    ).with(
                        Shape.BOX,
                        Color.BLACK,
                        Color.LIGHTSKYBLUE.fill(),
                    ).with(Font.name("Arial"))

        private val StateContainerBase<out IState>.standardNode: Node
            get() = standardNodeBase.with(Label.of(name))

        private val StateContainerBase<out IState>.parentNode: Node
            get() = standardNode.with(Label.html(name.nestedLabel))

        private fun StateContainerBase<out IState>.historyNode(isNested: Boolean): Node =
            standardNode.with(Label.html(name.historyLabel(isNested)))

        private fun StateContainerBase<out IState>.deepHistoryNode(isNested: Boolean): Node =
            standardNode.with(Label.html(name.deepHistoryLabel(isNested)))

        private fun StateContainerBase<out IState>.historyDeepHistoryNode(isNested: Boolean): Node =
            standardNode.with(Label.html(name.historyAndDeepHistoryLabel(isNested)))

        private val StateContainerBase<out IState>.initialNode: Node
            get() =
                node(id)
                    .with(
                        Style.FILLED,
                        Shape.CIRCLE,
                        Color.BLACK,
                        Label.of(""),
                    ).with(Size.mode(Size.Mode.FIXED).size(0.3, 0.3))

        private val StateContainerBase<out IState>.finalNode: Node
            get() =
                node(id)
                    .with(
                        Style.FILLED,
                        Shape.DOUBLE_CIRCLE,
                        Color.BLACK,
                        Label.of(""),
                    ).with(Size.mode(Size.Mode.FIXED).size(0.3, 0.3))

        val StateContainerInfo.node: Node
            get() =
                when {
                    container.state is InitialState -> container.initialNode
                    container.state is FinalState -> container.finalNode
                    hasHistory && hasDeepHistory -> container.historyDeepHistoryNode(container.hasChildren)
                    hasHistory && !hasDeepHistory -> container.historyNode(container.hasChildren)
                    !hasHistory && hasDeepHistory -> container.deepHistoryNode(container.hasChildren)
                    container.hasChildren -> container.parentNode
                    else -> container.standardNode
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

        private val ITransition.label
            get() =
                Label.of(
                    when (eventType) {
                        StartEvent::class, NoEvent::class -> ""
                        else -> eventType.simpleName ?: "Event"
                    },
                )

        private val String.nestedLabel
            get() =
                """
                <TABLE BORDER="0" CELLBORDER="0" CELLSPACING="0" CELLPADDING="0" style="font-size:2">
                  <TR><TD VALIGN="BOTTOM"><FONT>$this</FONT></TD></TR>
                  <TR><TD ALIGN="RIGHT"><FONT POINT-SIZE="6">o-o</FONT></TD></TR>
                </TABLE>
                """.trimIndent()

        private fun String.historyLabel(isNested: Boolean): String {
            val nested = if (isNested) "o-o" else ""
            return """
                <TABLE BORDER="0" CELLBORDER="0" CELLSPACING="0" CELLPADDING="0">
                  <TR>
                    <TD>
                      <TABLE BORDER="0" CELLBORDER="0" CELLSPACING="0" CELLPADDING="0" style="font-size:2">
                        <TR><TD><FONT POINT-SIZE="2"> </FONT></TD></TR>
                        <TR><TD PORT="hist" ALIGN="LEFT"><FONT POINT-SIZE="8">H</FONT></TD></TR>
                        <TR><TD><FONT POINT-SIZE="2"> </FONT></TD></TR>
                      </TABLE>
                    </TD>
                    <TD> </TD>
                    <TD>
                      <TABLE BORDER="0" CELLBORDER="0" CELLSPACING="0" CELLPADDING="0" style="font-size:2">
                        <TR><TD VALIGN="BOTTOM"><FONT>$this</FONT></TD></TR>
                        <TR><TD ALIGN="RIGHT"><FONT POINT-SIZE="6">$nested</FONT></TD></TR>
                      </TABLE>
                    </TD>
                  </TR>
                </TABLE>
                """.trimIndent()
        }

        private fun String.deepHistoryLabel(isNested: Boolean): String {
            val nested = if (isNested) "o-o" else ""
            return """
                <TABLE BORDER="0" CELLBORDER="0" CELLSPACING="0" CELLPADDING="0">
                  <TR>
                    <TD>
                      <TABLE BORDER="0" CELLBORDER="0" CELLSPACING="0" CELLPADDING="0" style="font-size:2">
                        <TR><TD><FONT POINT-SIZE="2"> </FONT></TD></TR>
                        <TR><TD PORT="deep" ALIGN="LEFT"><FONT POINT-SIZE="8">H*</FONT></TD></TR>
                        <TR><TD><FONT POINT-SIZE="2"> </FONT></TD></TR>
                      </TABLE>
                    </TD>
                    <TD> </TD>
                    <TD>
                      <TABLE BORDER="0" CELLBORDER="0" CELLSPACING="0" CELLPADDING="0" style="font-size:2">
                        <TR><TD VALIGN="BOTTOM"><FONT>$this</FONT></TD></TR>
                        <TR><TD ALIGN="RIGHT"><FONT POINT-SIZE="6">$nested</FONT></TD></TR>
                      </TABLE>
                    </TD>
                          </TR>
                        </TABLE>
                """.trimIndent()
        }

        private fun String.historyAndDeepHistoryLabel(isNested: Boolean): String {
            val nested = if (isNested) "o-o" else ""
            return """
                <TABLE BORDER="0" CELLBORDER="0" CELLSPACING="0" CELLPADDING="0">
                  <TR>
                    <TD>
                      <TABLE BORDER="0" CELLBORDER="0" CELLSPACING="0" CELLPADDING="0" style="font-size:2">
                        <TR><TD><FONT POINT-SIZE="2"> </FONT></TD></TR>
                        <TR><TD PORT="hist" ALIGN="LEFT"><FONT POINT-SIZE="8">H</FONT></TD></TR>
                        <TR><TD><FONT POINT-SIZE="2"> </FONT></TD></TR>
                        <TR><TD PORT="deep" ALIGN="LEFT"><FONT POINT-SIZE="8">H*</FONT></TD></TR>
                        <TR><TD><FONT POINT-SIZE="2"> </FONT></TD></TR>
                      </TABLE>
                    </TD>
                    <TD> </TD>
                    <TD>
                      <TABLE BORDER="0" CELLBORDER="0" CELLSPACING="0" CELLPADDING="0" style="font-size:2">
                        <TR><TD VALIGN="BOTTOM"><FONT>$this</FONT></TD></TR>
                        <TR><TD ALIGN="RIGHT"><FONT POINT-SIZE="6">$nested</FONT></TD></TR>
                      </TABLE>
                    </TD>
                  </TR>
                </TABLE>
                """.trimIndent()
        }

        private fun Node.use(transition: ITransition): Link =
            when (transition.endPoint.history) {
                History.H -> Factory.to(this.port("hist")).with(transition.label, Font.name("Arial"), Font.size(10))
                History.Hd -> Factory.to(this.port("deep")).with(transition.label, Font.name("Arial"), Font.size(10))
                else -> Factory.to(this).with(transition.label, Font.name("Arial"), Font.size(10))
            }
    }
}
