import React from "react";
import {Process} from "../generated/graphql";
import {Section, Level} from "react-bulma-components";
import {format} from "../utils/nanos";

type ViewState = {
  root: Process,
  current: Process
}

class Bar extends React.Component<ViewState> {

  static key = 0

  render() {
    const {current, root} = this.props;
    const left = ((current.start / this.props.root.duration) * 100).toFixed(1)
    const width = ((current.duration / this.props.root.duration) * 100).toFixed(1)

    const startTime = format(current.start)
    const duration = format(current.duration)

    return <>
      <Level justifyContent={'flex-start'}>
        <div style={{flexBasis: `${left}%`}}/>
        <div className="process-bar" style={{flexBasis: `${width}%`}}>{startTime} - {duration}</div>
      </Level>
      {current.children?.map(child => {
        return <Bar key={Bar.key++} root={this.props.root} current={child}/>
      })}
    </>;
  }
}

export class ProcessView extends React.Component<ViewState> {
  render() {
    console.log(this.props.current)
    return <Section>
      <Bar root={this.props.root} current={this.props.current}/>
    </Section>;
  }
}