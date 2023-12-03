package com.sericulture.marketandauction.helper;

public final class MAConstants {

    public static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal Server Error - Error occurred while processing the request.";

    public static final String INTERNAL_SERVER_ERROR_OUTPUT_FORMAT = """
                    {
                      "content": null,
                      "errorMessages": [
                        {
                          "errorType": "INTERNAL_SERVER_ERROR",
                          "message": [
                            {
                              "message": "Internal Server Error - Error occurred while processing the request.",
                              "label": "SYSTEM",
                              "locale": "English",
                              "reason": "JSON parse error: Cannot deserialize value of type `com.sericulture.marketandauction.helper.MarketAuctionHelper$activityType`"
                            }
                          ]
                        }
                      ],
                      "errorCode": 500
                    }
            """;
    /**FLEX TIME CONTROLLER BEGIN **/
    public static final String VALIDATION_MESSAGE_FLEX_TIME = """
            {
              "content": null,
              "errorMessages": [
                {
                  "errorType": "VALIDATION",
                  "message": [
                    {
                      "message": "InValid Id, Please enter valid Id",
                      "label": "NON_LABEL_MESSAGE",
                      "locale": "English",
                      "errorCode": ""
                    }
                  ]
                }
              ],
              "errorCode": 0
            }
            """;
    /**FLEX TIME CONTROLLER ENDS **/

    public static final String ALL_SLIPS_STATUS_AND_AUC_DATE_OUTPUT = """
            {
              "content": [
                {
                  "transactionId": 95,
                  "marketId": 1,
                  "godownId": 0,
                  "farmerId": 104,
                  "allotedLotList": [],
                  "allotedSmallBinList": [],
                  "allotedBigBinList": []
                }
              ],
              "errorMessages": [],
              "errorCode": 0
            }
            """;
    public static final String ALL_SLIPS_MESSAGE_FARMER_AUC_DATE_OUTPUT = """
            {
              "content": [
                {
                  "transactionId": 1225,
                  "marketId": 9,
                  "godownId": 0,
                  "farmerId": 123,
                  "allotedLotList": [],
                  "allotedSmallBinList": [],
                  "allotedBigBinList": []
                },
                {
                  "transactionId": 1226,
                  "marketId": 9,
                  "godownId": 0,
                  "farmerId": 123,
                  "allotedLotList": [],
                  "allotedSmallBinList": [],
                  "allotedBigBinList": []
                }
              ],
              "errorMessages": [],
              "errorCode": 0
            }
            """;
    public static final String VALIDATION_ERROR_LOT_ALLOTMENT = """
            {"errorType":"VALIDATION","message":[{"message":"Title should be more than 1 characters.","label":"name","locale":null}]}
            """;

    public static final String SUCCESS_LOT_ALLOTMENT_OUTPUT = """
            {
                "content": {
                    "transactionId": 9,
                    "marketId": 1,
                    "godownId": 0,
                    "farmerId": 1,
                    "allotedLotList": [
                        1,
                        2,
                        3
                    ],
                    "allotedSmallBinList": [
                        5,
                        6,
                        7,
                        8
                    ],
                    "allotedBigBinList": [
                        4,
                        5
                    ]
                },
                "errorMessages": []
            }
            """;
}
