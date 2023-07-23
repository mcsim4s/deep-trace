import React from "react";
import {ParallelProcess, Process} from "../generated/graphql";
import {Level, Columns} from "react-bulma-components";
import {format} from "../utils/nanos";
import {Trace} from "../utils/Trace";

type ViewProps = {
    trace: Trace,
    current: Process,
    parentStart: number,
    depth: number
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

    private nameLegend(service: string, operation: string): JSX.Element {
        return <>
            <p><strong>{service}</strong> <small>{operation}</small></p>
        </>;
    }

    private legend(process: Process): JSX.Element {
        switch (process.__typename) {
            case "SequentialProcess":
                if (process.childrenIds.length === 3) {
                    const only = this.props.trace.get(process.childrenIds[1])
                    const service = this.props.trace.serviceOf(only);
                    const operation = this.props.trace.operationOf(only);
                    return this.nameLegend(service, operation)
                } else {
                    return <>SequentialProcess</>;
                }
            case "Gap":
                return <>Gap</>;
            case "ConcurrentProcess":
                const of = (this.props.trace.get(process.ofId)) as ParallelProcess;
                return this.nameLegend(of.service, of.operation);
            case "ParallelProcess":
                return this.nameLegend(process.service, process.operation);

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
                const width = ((process.stats.duration.average / rootDuration) * 100).toFixed(2)
                return [
                    <div onClick={() => this.toggleStatsFor(process.id)} style={{flexBasis: `${width}%`}}
                         className={"stats-toggler"}>
                        <div key={process.id} className="process-bar gap"/>
                    </div>
                ];
            }
            case "ConcurrentProcess": {
                const width = Math.max((process.stats.flat.duration.average / rootDuration) * 100, 0.3).toFixed(2)
                return [
                    <div onClick={() => this.toggleStatsFor(process.id)} style={{flexBasis: `${width}%`}}
                         className={"stats-toggler"}>
                        <div key={process.id} className="process-bar concurrent"/>
                    </div>
                ];
            }
            case "ParallelProcess": {
                const width = Math.max((process.stats.duration.average / rootDuration) * 100, 0.3).toFixed(2)
                return [
                    <div onClick={() => this.toggleStatsFor(process.id)} style={{flexBasis: `${width}%`}}
                         className={"stats-toggler"}>
                        <div key={process.id} className="process-bar parallel"/>
                    </div>
                ];
            }
            default:
                throw new Error("Unknown process type");
        }
    }

    private children(process: Process, parentStart: number): JSX.Element[] {
        const trace = this.props.trace;
        switch (process.__typename) {
            case "SequentialProcess": {
                const children = this.props.trace.childrenOf(process);
                const withoutGaps = children.filter(el => el.__typename !== "Gap")
                if (withoutGaps.length === 1) {
                    return this.children(withoutGaps[0], trace.flatStatsOf(children[0]).duration.average + parentStart)
                } else {
                    let acc = 0;
                    return withoutGaps.map(child => {
                        const start = parentStart + acc;
                        acc = acc + trace.flatStatsOf(child).duration.average
                        return <Bar key={child.id}
                                    trace={this.props.trace}
                                    current={child} parentStart={start}
                                    depth={this.props.depth + 1}/>
                    })
                }
            }
            case "Gap": {
                return [];
            }
            case "ConcurrentProcess": {
                const of = this.props.trace.get(process.ofId) as ParallelProcess;
                const stats = process.stats;
                const count = Math.ceil(stats.avgSubprocesses);
                const add = (process.stats.flat.duration.average - of.stats.duration.average) / (count - 1);
                const result = [];
                for (let i = 0; i < count; i++) {
                    result.push(
                        <Bar key={`${of.id}-${i}`}
                             trace={this.props.trace}
                             current={of}
                             parentStart={parentStart + add * i}
                             depth={this.props.depth + 1}/>
                    );
                }
                return result;
            }
            case "ParallelProcess": {
                return this.props.trace.childrenOf(process).map(child => {
                    return <Bar key={child.id}
                                trace={this.props.trace}
                                current={child}
                                parentStart={parentStart}
                                depth={this.props.depth + 1}/>
                })
            }
            default:
                throw new Error("Unknown process type");
        }
    }

    private statsSection() {
        const trace = this.props.trace;
        if (this.state.showStatsFor) {
            const stats = trace.flatStatsOf(trace.get(this.state.showStatsFor))
            return <Columns>
                <Columns.Column size={LegendSize} className="process-legend">
                </Columns.Column>
                <Columns.Column>
                    {format(stats.duration.average)}
                </Columns.Column>
            </Columns>;
        } else return <></>;

    }

    render() {
        const {current, depth, parentStart} = this.props;
        const root = this.props.trace.root
        const left = ((parentStart / root.stats.duration.average) * 100).toFixed(2)
        const tabs = [];
        for (let i = 0; i < depth; i++) {
            tabs.push(<div className="trace-title-tab"/>);
        }

        return <>
            {/*Process Bar*/}
            <Columns className="process-container">
                <Columns.Column size={LegendSize} className="process-legend">
                    <Level className="is-justify-content-flex-start is-align-items-stretch">
                        {tabs}
                        {this.legend(current)}
                    </Level>
                </Columns.Column>
                <Columns.Column className="process-bar-container">
                    <Level justifyContent={'flex-start'}>
                        <div style={{flexBasis: `${left}%`}}/>
                        {this.bars(current, root.stats.duration.average)}
                    </Level>
                </Columns.Column>
            </Columns>

            {/*Process Stats*/}
            {this.statsSection()}

            {/*Children*/}
            {this.state.collapsed ? [] : this.children(current, parentStart)}
        </>;
    }
}

export class ProcessView extends React.Component<{ trace: Trace }> {
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
            <Bar trace={this.props.trace}
                 current={this.props.trace.root}
                 key={this.props.trace.root.id}
                 parentStart={0}
                 depth={0}/>
        </>;
    }
}