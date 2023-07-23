import {gql, useQuery} from "@apollo/client";
import {ReportFragment} from "../gql/fragments";
import {Queries, QueriesGetReportArgs} from "../generated/graphql";
import {Box, Heading} from "react-bulma-components";
import React from "react";
import {Link, useNavigate, useParams} from "react-router-dom";

const getReportQuery = gql`
    ${ReportFragment}

    query GetReport($value: String!) {
        getReport(value: $value) {
            ...Report
        }
    }
`;

function ReportPage() {
    let { reportId } = useParams<{reportId: string}>();
    if (!reportId) throw new Error("Report page was rendered without report id param");
    let { loading, error, data } = useQuery<Queries, QueriesGetReportArgs>(getReportQuery,
        { variables: { value: reportId } }
    );
    if (error) throw error;
    if (loading) return <Heading size={4}>Loading ...</Heading>;
    if (!data || !data.getReport) throw new Error("no data");

    let body;
    switch (data.getReport?.state.__typename) {
        case "ClustersBuilt":
            body = <>
                <Heading subtitle={true}>Report Clusters:</Heading>
                {data.getReport.state.clusterIds.map(ref => {
                    return <Link to={`/cluster/${ref.id.reportId}/${ref.id.structureHash}`} key={ref.id.structureHash}>
                        <Box>{ref.id.structureHash}</Box>
                    </Link>
                })}
            </>;
            break;
        default:
            body = <Heading size={5}>Clustering</Heading>
    }

    return <>
        <Heading size={2}>{data.getReport?.service} :: {data.getReport?.operation}</Heading>
        {body}
    </>;
}

export default ReportPage;