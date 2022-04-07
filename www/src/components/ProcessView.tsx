import React from "react";
import {Process} from "../generated/graphql";
import {Section, Level, Columns} from "react-bulma-components";
import {format} from "../utils/nanos";
import {Trace} from "../utils/Trace";

type ViewState = {
  trace: Trace,
  current: Process
}
const LegendSize = 2;

class Bar extends React.Component<ViewState> {

  render() {
    const {current, trace} = this.props;
    const left = ((current.start / this.props.trace.root.duration) * 100).toFixed(1)
    let  width = ((current.duration / this.props.trace.root.duration) * 100).toFixed(1)

    const startTime = format(current.start)
    const duration = format(current.duration)

    return <>
      <Columns>
        <Columns.Column size={LegendSize} className="process-legend">
          <Level>{current.service}  -  {current.operation}</Level>
        </Columns.Column>
        <Columns.Column className="process-bar-container">
          <Level justifyContent={'flex-start'}>
            <div style={{flexBasis: `${left}%`}}/>
            <div className="process-bar" style={{flexBasis: `${width}%`}}>{duration}</div>
          </Level>
        </Columns.Column>
      </Columns>
      {trace.childrenOf(current).map(child => {
        return <Bar key={child.id} trace={this.props.trace} current={child}/>
      })}
    </>;
  }
}

export class ProcessView extends React.Component<ViewState> {
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