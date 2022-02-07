import {gql, useQuery} from "@apollo/client";
import {Queries} from "../generated/graphql";
import {Columns, Heading} from "react-bulma-components";
import React from "react";
import ReportCreateForm from "../components/ReportCreateForm";

function ScenarioPage() {
  let { loading, error, data } = useQuery<Queries>(
    gql`
      query {
        listReports {
          id,
          name
        }
      }
    `,
  );
  if (error) throw error;

  return <>
    <Columns>
      <Columns.Column size={'one-third'}>
        <ReportCreateForm/>
      </Columns.Column>
      <Columns.Column>
        {loading ? (<Heading size={4}>Loading ...</Heading>) : (
          <Heading size={4}>REPORTS</Heading>
        )}
      </Columns.Column>
    </Columns>
  </>;
}

export default ScenarioPage;