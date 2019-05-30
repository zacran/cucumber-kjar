Feature: Validate Rules
    Scenario: Fresh and Valid Cabbage Kimchi
        Given I have the following kimchi:
            | Ingredients            | Age |
            | Cabbage, Spices, Water | 5   |
        When I attempt to make Kimchi
        Then The Kimchi will taste great!

    Scenario: Fresh and Valid Radish Kimchi
        Given I have the following kimchi:
            | Ingredients           | Age |
            | Radish, Spices, Water | 5   |
        When I attempt to make Kimchi
        Then The Kimchi will taste great!

    Scenario: Invalid Ingredients
        Given I have the following kimchi:
            | Ingredients   | Age |
            | Spices, Water | 1   |
        When I attempt to make Kimchi
        Then It will not be Kimchi!

    Scenario: Old Kimchi But Still Tastes Good
        Given I have the following kimchi:
            | Ingredients            | Age |
            | Cabbage, Spices, Water | 120 |
        When I attempt to make Kimchi
        Then The Kimchi will taste great!

    Scenario: Old Kimchi
        Given I have the following kimchi:
            | Ingredients            | Age |
            | Cabbage, Spices, Water | 150 |
        When I attempt to make Kimchi
        Then The Kimchi will not taste great.
