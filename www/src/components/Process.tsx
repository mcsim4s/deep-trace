import React from 'react';
import { useQuery, gql } from '@apollo/client';
import { Heading } from 'react-bulma-components';
import {ClusterQueries, ClusterQueriesGetArgs} from "../generated/graphql";

export default function Process() {
  let { loading, error, data } = useQuery<ClusterQueries, ClusterQueriesGetArgs>(
    gql`
      query GetCluster($value: String!) {
          get(value: $value) {
            reportId,
            structureHash
          }
      }
    `,
    { variables: { value: "test" } }
  );
  if (error) throw error;
  if (loading) return <Heading size={4}>Loading ...</Heading>;
  if (!data) throw new Error("no data");

  return <>
    <div>
      <Heading size={1}>Cluster</Heading>
      <Heading size={4}>Cluster id: {data.get.structureHash}</Heading>
    </div>
  </>;
};