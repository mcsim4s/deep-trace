import {Block, Box, Button, Columns, Heading, Panel, Section} from "react-bulma-components";
import React from "react";
import {gql, useQuery} from "@apollo/client";
import {AnalysisReport, Queries, Scalars, Maybe} from "../generated/graphql";
import {ApolloQueryResult} from "@apollo/client/core/types";
import {ReportFragment} from "../gql/fragments";
import {Link} from "react-router-dom";

const listReportsQuery = gql`
  ${ReportFragment}
  
  query ListReports {
    listReports {
      ...Report
    }
  }
`;

type ReportListProps = {
  whenToRefetch: (refetch: (variables?: Partial<any>) => Promise<ApolloQueryResult<Queries>>) => void ;
}

export default function ReportsListView(props: ReportListProps) {
  let { loading, error, data, refetch } = useQuery<Queries>(
    listReportsQuery
  )

  props.whenToRefetch(refetch);

  if (error) throw error;
  if (loading) return <Heading>Loading ...</Heading>;
  if (!data) throw new Error("No Data");


  return <>
    <Heading size={2}>Existing reports</Heading>
    <Block>
      {data.listReports?.map((report) => {
        const created = new Date(Date.parse(report.createdAt));
        return <Link to={`/report/${report.id}`} key={report.id} className="box">
          {report.service} --- {report.operation} --- {created.toLocaleString()} -------- {report.state.__typename}
        </Link>
      })}
    </Block>
    <br/>
  </>;
}