import React from "react";
import {FlatStats, ParallelProcess, Process} from "../generated/graphql";
import {Section, Level, Columns} from "react-bulma-components";
import {format} from "../utils/nanos";
import {Trace} from "../utils/Trace";
import ProcessUtils from "../utils/ProcessUtils";

type ViewProps = {
  trace: Trace,
  current: Process,
  parentStart: number
}

type ViewState = {
  showStatsFor: string | undefined,
  collapsed: Boolean
}

const LegendSize = 2;

class Bar extends React.Component<ViewProps, ViewState> {
  constructor(props: Readonly<ViewProps> | ViewProps) {
    super(props);
    this.state = {
      showStatsFor: undefined,
      collapsed: false
      // collapsed: props.current.__typename === "SequentialProcess"
    }
  }

  toggleStatsFor(processId: string) {
    this.setState({
      showStatsFor: this.state.showStatsFor ? undefined : processId,
      collapsed: this.state.collapsed
    })
  }

  private legend(process: Process): String {
    switch (process.__typename) {
      case "SequentialProcess":
        if (process.childrenIds.length === 3) {
          const only = ProcessUtils.asParallel(this.props.trace.get(process.childrenIds[1]));
          return `${only.service} - ${only.operation}`;
        } else {
          return "SequentialProcess"
        }
      case "Gap":
        return "Gap";
      case "ConcurrentProcess":
        const of = this.props.trace.get(process.ofId) as ParallelProcess;
        return `${of.service} - ${of.operation}`;
      case "ParallelProcess":
        return `${process.service} - ${process.operation}`;

      default:
        throw new Error("Unknown process type");
    }
  }

  private bars(process: Process, rootDuration: number): JSX.Element[] {
    switch (process.__typename) {
      case "SequentialProcess": {
        return this.props.trace.childrenOf(process).flatMap(c => this.bars(c, rootDuration));
      }
      case "Gap": {
        const width = ((process.stats.avgDuration / rootDuration) * 100).toFixed(2)
        return [
          <div onClick={() => this.toggleStatsFor(process.id)} style={{flexBasis: `${width}%`}} className={"stats-toggler"}>
            <div key={process.id} className="process-bar gap"/>
          </div>
        ];
      }
      case "ConcurrentProcess":{
        const width = ((process.stats.flat.avgDuration / rootDuration) * 100).toFixed(2)
        return [
          <div onClick={() => this.toggleStatsFor(process.id)} style={{flexBasis: `${width}%`}} className={"stats-toggler"}>
            <div key={process.id} className="process-bar concurrent"/>
          </div>
        ];
      }
      case "ParallelProcess": {
        const width = ((process.stats.avgDuration / rootDuration) * 100).toFixed(2)
        return [
          <div onClick={() => this.toggleStatsFor(process.id)} style={{flexBasis: `${width}%`}} className={"stats-toggler"}>
            <div key={process.id} className="process-bar parallel"/>
          </div>
        ];
      }
      default:
        throw new Error("Unknown process type");
    }
  }

  private children(process: Process, parentStart: number): JSX.Element[] {
    switch (process.__typename) {
      case "SequentialProcess": {
        const children = this.props.trace.childrenOf(process);
        const withoutGaps = children.filter(el => el.__typename !== "Gap")
        if (withoutGaps.length === 1) {
          return this.children(withoutGaps[0], ProcessUtils.flatStatsOf(children[0]).avgDuration + parentStart)
        } else {
          return withoutGaps.map(child => <Bar key={child.id} trace={this.props.trace} current={child} parentStart={ProcessUtils.flatStatsOf(child).avgStart + parentStart}/>)
        }
      }
      case "Gap": {
        return [];
      }
      case "ConcurrentProcess": {
        const of = this.props.trace.get(process.ofId) as ParallelProcess;
        const add = (process.stats.flat.avgDuration - of.stats.avgDuration) / 2;
        return [<Bar key={of.id} trace={this.props.trace} current={of} parentStart={parentStart + add}/>];
      }
      case "ParallelProcess": {
        return this.props.trace.childrenOf(process).map(child => {
          return <Bar key={child.id} trace={this.props.trace} current={child} parentStart={parentStart}/>
        })
      }
      default:
        throw new Error("Unknown process type");
    }
  }

  render() {
    const {current, trace, parentStart} = this.props;
    const root = this.props.trace.root
    const left = ((parentStart / root.stats.avgDuration) * 100).toFixed(2)

    let children: JSX.Element[] = [];
    if (!this.state.collapsed) {
      children = this.children(current, parentStart)
    }
    let statsElement: JSX.Element[] = [];
    if (this.state.showStatsFor) {
      const stats = ProcessUtils.flatStatsOf(trace.get(this.state.showStatsFor))
      statsElement = [
        <Columns>
          <Columns.Column size={LegendSize} className="process-legend">
          </Columns.Column>
          <Columns.Column>
            {stats.allDurations.map(format).join(",")} : {format(stats.avgDuration)}
          </Columns.Column>
        </Columns>
      ];
    }

    return <>
      {/*Process Bar*/}
      <Columns className="process-container">
        <Columns.Column size={LegendSize} className="process-legend">
          <Level>{this.legend(current)}</Level>
        </Columns.Column>
        <Columns.Column className="process-bar-container">
          <Level justifyContent={'flex-start'}>
            <div style={{flexBasis: `${left}%`}}/>
            {this.bars(current, root.stats.avgDuration)}
          </Level>
        </Columns.Column>
      </Columns>

      {/*Process Stats*/}
      {statsElement}

      {/*Children*/}
      {children}
    </>;
  }
}

export class ProcessView extends React.Component<{trace: Trace}> {
  render() {
    return <>
      <Columns>
        <Columns.Column size={LegendSize} className="process-heading">
          <Level>Service Name - Operation</Level>
        </Columns.Column>
        <Columns.Column className="process-heading">
          <Level justifyContent={'flex-start'}>
          </Level>
        </Columns.Column>
      </Columns>
      <Bar trace={this.props.trace} current={this.props.trace.root} key={this.props.trace.root.id} parentStart={0}/>
    </>;
  }
}