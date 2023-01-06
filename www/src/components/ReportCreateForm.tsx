import {Box, Button, Columns, Form as BulmaForm, Heading, Panel} from "react-bulma-components";
import React from "react";
import {Form, Field, Formik} from "formik";
import {gql, useMutation, useQuery} from "@apollo/client";
import {MutationsCreateReportArgs, Mutations, Scalars, Maybe, Queries, QueriesSuggestArgs} from "../generated/graphql";
import {ReportFragment} from "../gql/fragments";
import {ReactComponent} from "*.svg";
import {FieldProps} from "formik/dist/Field";

type CreateReportParam = {
    serviceName: string;
    operationName: string;
    tags?: string;
    minDuration?: string;
    maxDuration?: string;
    lookup: "1h" | "2h";
}
let initialValues: CreateReportParam = {
    serviceName: "",
    operationName: "",
    lookup: "1h"
};

const createReportMutation = gql`
  ${ReportFragment}
  mutation CreateReport($sourceId: String!, $params: TraceQueryInput!) {
    createReport(sourceId: $sourceId, params: $params) {
      ...Report
    }
  }
`;

const getSuggestQuery = gql`
    query Suggest($serviceName: String!) {
      suggest(serviceName: $serviceName) {
        services,
        operations {
          name,
          kind
        }
      }
    }
`;

type AddReportFormProps = {
    onSubmit: () => any
}

export default function ReportCreateForm(props: AddReportFormProps) {
    const [send] = useMutation<Mutations, MutationsCreateReportArgs>(
        createReportMutation
    )
    let {data, refetch} = useQuery<Queries, QueriesSuggestArgs>(getSuggestQuery, {
        variables: {
            serviceName: null
        }
    })

    const services: string[] = data?.suggest.services || ["loading..."];
    const operations: string[] = data?.suggest.operations.map(o => o.name) || ["loading..."];

    initialValues.serviceName = services[0];
    initialValues.operationName = operations[0];
    return <Box className="is-primary">
        <Heading size={4} spaced={true}>Create new report</Heading>
        <hr/>
        <Formik initialValues={initialValues} onSubmit={(values, actions) => {
            actions.setSubmitting(false)
            const nowMillis = Math.floor(Date.now() / 1000);
            console.log({
                serviceName: values.serviceName,
                operationName: values.operationName,
                tags: Array.of<string>(...(values.tags?.split(' ') || [])),
                startTimeMinSeconds: nowMillis - 60 * 60,
                startTimeMaxSeconds: nowMillis
            });
            send({
                    variables: {
                        sourceId: "nevermind",
                        params: {
                            serviceName: values.serviceName,
                            operationName: values.operationName,
                            tags: Array.of<string>(...(values.tags?.split(' ') || [])),
                            startTimeMinSeconds: nowMillis - 60 * 60,
                            startTimeMaxSeconds: nowMillis
                        }
                    }
                }
            ).then(() => props.onSubmit())
        }}>
            <Form>
                <BulmaForm.Field>
                    <BulmaForm.Label>Service name</BulmaForm.Label>
                    <BulmaForm.Control>
                        <Field name="serviceName" value={initialValues.serviceName}>
                            {(props: FieldProps) => {
                                return <BulmaForm.Select onChange={(event) => {
                                    props.field.onChange(event);
                                    refetch({
                                        serviceName: event.currentTarget.value
                                    });
                                }} value={services[0]}>
                                    {services.map(service => {
                                        return <option key={`${service}-option`} value={service}>{service}</option>
                                    })}
                                </BulmaForm.Select>
                            }}
                        </Field>
                    </BulmaForm.Control>
                </BulmaForm.Field>
                <BulmaForm.Field>
                    <BulmaForm.Label>Operation</BulmaForm.Label>
                    <BulmaForm.Control>
                        <div className={"select"}>
                            <Field as="select" name="operationName">
                                {operations.map(operation => {
                                    return <option key={`${operation}-option`} value={operation}>{operation}</option>
                                })}
                            </Field>
                        </div>
                    </BulmaForm.Control>
                </BulmaForm.Field>
                <BulmaForm.Field>
                    <BulmaForm.Label>Tags</BulmaForm.Label>
                    <BulmaForm.Control>
                        <Field className={"input"} name={"tags"}/>
                    </BulmaForm.Control>
                </BulmaForm.Field>
                <BulmaForm.Field>
                    <BulmaForm.Label>Lookback</BulmaForm.Label>
                    <BulmaForm.Control>
                        <div className={"select"}>
                            <Field as="select" name="lookup">
                                <option value="1h">Last Hour</option>
                                <option value="2h">Last 2 Hours</option>
                            </Field>
                        </div>
                    </BulmaForm.Control>
                </BulmaForm.Field>
                <Columns>
                    <Columns.Column size="half">
                        <BulmaForm.Field>
                            <BulmaForm.Label>Max Duration</BulmaForm.Label>
                            <BulmaForm.Control>
                                <Field className={"input"} name={"maxDuration"}/>
                            </BulmaForm.Control>
                        </BulmaForm.Field>
                    </Columns.Column>
                    <Columns.Column size="half">
                        <BulmaForm.Field>
                            <BulmaForm.Label>Min Duration</BulmaForm.Label>
                            <BulmaForm.Control>
                                <Field className={"input"} name={"minDuration"}/>
                            </BulmaForm.Control>
                        </BulmaForm.Field>
                    </Columns.Column>
                </Columns>
                <BulmaForm.Field>
                    <BulmaForm.Control>
                        <Button className="is-success" submit={true}>Create report</Button>
                    </BulmaForm.Control>
                </BulmaForm.Field>
            </Form>
        </Formik>
    </Box>;
}