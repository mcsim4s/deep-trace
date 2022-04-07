import React from "react";
import {Process} from "../generated/graphql";
import {Section, Level, Columns} from "react-bulma-components";
import {format} from "../utils/nanos";
import {Trace} from "../utils/Trace";

type ViewProps = {
  trace: Trace,
  current: Process
}

type ViewState = {
  statsHidden: Boolean
}

const LegendSize = 2;

class Bar extends React.Component<ViewProps, ViewState> {


  constructor(props: Readonly<ViewProps> | ViewProps) {
    super(props);
    this.state = {
      statsHidden: true
    }
  }

  toggleStats() {
    this.setState({
      statsHidden: !this.state.statsHidden
    })
  }

  render() {
    const {current, trace} = this.props;
    const root = this.props.trace.root
    const left = ((current.stats.avgStart / root.stats.avgDuration) * 100).toFixed(1)
    let width = ((current.stats.avgDuration / root.stats.avgDuration) * 100).toFixed(1)

    const duration = format(current.stats.avgDuration)
    const avg = format(current.stats.allDurations.reduce((a, b) => a + b) / current.stats.allDurations.length)

    return <>
      {/*Process Bar*/}
      <Columns onClick={this.toggleStats.bind(this)} className="process-container">
        <Columns.Column size={LegendSize} className="process-legend">
          <Level>{current.service} - {current.operation}</Level>
        </Columns.Column>
        <Columns.Column className="process-bar-container">
          <Level justifyContent={'flex-start'}>
            <div style={{flexBasis: `${left}%`}}/>
            <div className="process-bar" style={{flexBasis: `${width}%`}}>{duration}</div>
          </Level>
        </Columns.Column>
      </Columns>

      {/*Process Stats*/}
      <Columns className={this.state.statsHidden ? "is-hidden" : ""}>
        <Columns.Column size={LegendSize} className="process-legend">
        </Columns.Column>
        <Columns.Column>
          {current.stats.allDurations.map(format).join(",")} : {avg}
        </Columns.Column>
      </Columns>
      {trace.childrenOf(current).map(child => {
        return <Bar key={child.id} trace={this.props.trace} current={child}/>
      })}
    </>;
  }
}

export class ProcessView extends React.Component<ViewProps> {
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
      <Bar trace={this.props.trace} current={this.props.current} key={this.props.trace.root.id}/>
    </>;
  }
}