import React from "react";
import {Columns, Level} from "react-bulma-components";
import {Trace} from "../utils/Trace";
import {Bar} from "./Bar";

const LegendSize = 2;

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