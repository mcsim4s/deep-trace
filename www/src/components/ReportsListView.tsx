import {Box, Button, Columns, Heading, Panel, Section} from "react-bulma-components";
import React from "react";
import {gql, useQuery} from "@apollo/client";
import {AnalysisReport, Queries, Scalars, Maybe} from "../generated/graphql";
import {ApolloQueryResult} from "@apollo/client/core/types";
import {ReportFragment} from "../gql/fragments";

type CreateReportParam = {
  serviceName: string;
  operationName: string;
  tags?: string;
  minDuration?: string;
  maxDuration?: string;
  lookup: "1h" | "2h";
}
const initialValues: CreateReportParam = {
  serviceName: "jaeger-query",
  operationName: "/api/traces",
  lookup: "1h"
};


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

  return <>
    <Heading size={2}>Existing reports</Heading>
    {data?.listReports?.map((report) => {
      return <Box key={report.id}>
        {report.createdAt}
      </Box>
    })}
    <br/>

  </>;
}