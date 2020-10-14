# coding: utf-8

from __future__ import absolute_import

from flask import json
from six import BytesIO

from swagger_server.models.entrypoint_message import EntrypointMessage  # noqa: E501
from swagger_server.models.entrypoint_response import EntrypointResponse  # noqa: E501
from swagger_server.models.status_response import StatusResponse  # noqa: E501
from swagger_server.test import BaseTestCase


class TestDefaultController(BaseTestCase):
    """DefaultController integration test stubs"""

    def test_kairos_entrypoint_post(self):
        """Test case for kairos_entrypoint_post

        Performer algorithm execution entry point.
        """
        body = EntrypointMessage()
        response = self.client.open(
            '/kairos/entrypoint',
            method='POST',
            data=json.dumps(body),
            content_type='application/json')
        self.assert200(response,
                       'Response body is : ' + response.data.decode('utf-8'))

    def test_kairos_status_get(self):
        """Test case for kairos_status_get

        Returns current processing state of performer container.
        """
        response = self.client.open(
            '/kairos/status',
            method='GET')
        self.assert200(response,
                       'Response body is : ' + response.data.decode('utf-8'))


if __name__ == '__main__':
    import unittest
    unittest.main()
