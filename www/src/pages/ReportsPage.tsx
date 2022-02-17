import {Columns} from "react-bulma-components";
import React from "react";
import ReportCreateForm from "../components/ReportCreateForm";
import ReportsListView from "../components/ReportsListView";
import {ApolloQueryResult} from "@apollo/client/core/types";
import {Queries} from "../generated/graphql";

export default class ReportsPage extends React.Component {
  constructor(props: {}) {
    super(props);
    this.state = {};
  }

  render() {
    let refetchFunc: ((variables?: Partial<any>) => Promise<ApolloQueryResult<Queries>>) | undefined = undefined;

    const onSubmit = () => {
      refetchFunc && refetchFunc();
    };

    return <>
      <Columns>
        <Columns.Column size={'one-third'}>
          <ReportCreateForm onSubmit={onSubmit}/>
        </Columns.Column>
        <Columns.Column>
          <ReportsListView whenToRefetch={(x) => refetchFunc = x}/>
        </Columns.Column>
      </Columns>
    </>;
  }
}