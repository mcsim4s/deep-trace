import React from 'react';
import { useQuery, gql } from '@apollo/client';
import { Heading } from 'react-bulma-components';
import {Queries, QueriesGetClusterArgs} from "../generated/graphql";
import {ProcessView} from "./ProcessView";

export default function Cluster() {
  let { loading, error, data } = useQuery<Queries, QueriesGetClusterArgs>(
    gql`
      fragment ProcessFields on Process {
        start,
        duration
      }
      
      fragment ProcessRecursive on Process {
        ...ProcessFields,
        children {
          ...ProcessFields
        }
      }
      
      query GetCluster($reportId: String!, $structureHash: String!) {
        getCluster(reportId: $reportId, structureHash: $structureHash) {
          clusterId {
            reportId,
            structureHash
          },
          rootProcess {
            ...ProcessRecursive
          }
        }
      }
    `,
    { variables: { reportId: "test", structureHash: "test" } }
  );
  if (error) throw error;
  if (loading) return <Heading size={4}>Loading ...</Heading>;
  if (!data) throw new Error("no data");

  return <>
    <div>
      <Heading size={1}>Cluster</Heading>
      <Heading size={4}>Report id: {data.getCluster.clusterId.reportId}</Heading>
      <ProcessView root={data.getCluster.rootProcess} current={data.getCluster.rootProcess}/>
    </div>
  </>;
};