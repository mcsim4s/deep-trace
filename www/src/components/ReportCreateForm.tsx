import {Box, Button, Columns, Form as BulmaForm, Heading, Panel} from "react-bulma-components";
import React from "react";
import {Form, Field, Formik} from "formik";
import {gql, useMutation} from "@apollo/client";
import {MutationsCreateReportArgs, Mutations, Scalars, Maybe} from "../generated/graphql";

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


const createReportMutation = gql`
  mutation CreateReport($sourceId: String!, $params: TraceQueryInput!) {
    createReport(sourceId: $sourceId, params: $params) {
      id,
      name
    }
  }
`;

export default function ReportCreateBulmaForm() {
  const [send] = useMutation<Mutations, MutationsCreateReportArgs>(
    createReportMutation
  )


  return <Box className="is-primary">
    <Heading size={4} spaced={true}>Create new report</Heading>
    <hr/>
    <Formik initialValues={initialValues} onSubmit={(values, actions) => {
      actions.setSubmitting(false)
      alert(JSON.stringify(values, null, 2));
      const nowMillis = Math.floor(Date.now() / 1000);
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
      )
    }}>
    <Form>
      <BulmaForm.Field>
        <BulmaForm.Label>Service name</BulmaForm.Label>
        <BulmaForm.Control>
          <Field className={"input"} name={"serviceName"}/>
        </BulmaForm.Control>
      </BulmaForm.Field>
      <BulmaForm.Field>
        <BulmaForm.Label>Operation</BulmaForm.Label>
        <BulmaForm.Control>
          <Field className={"input"} name={"operationName"}/>
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