All strikethroughs represent soft deletes

## Initial State

| Name      | Value |
|-----------|-------|
| firstName | Joe   |

        v1.addPropertyValue("k1", "firstName", "Joe", V(""))

| RK | Vis | t | CF    | CQ              | Value  |
|----|-----|---|-------|-----------------|--------|
| v1 |     | 1 | PROP  | firstName\x1fk1 | Joe    |
        
## Case Scoped Change

| Name      | Value  |                 |
|-----------|--------|-----------------|
| firstName | Joe    | Hidden(WS1)     |
| firstName | Joseph | Visibility(WS1) |

        v1.markPropertyHidden("k1", "firstName", V("WS1")) 
        v1.addPropertyValue("k1", "firstName", "Joseph", V("WS1"))

| RK | Vis | t | CF    | CQ                     | Value  |
|----|-----|---|-------|------------------------|--------|
| v1 |     | 1 | PROP  | firstName\x1fk1        | Joe    |
| v1 |     | 2 | PROPH | firstName\x1fk1\x1fWS1 |        |
| v1 | WS1 | 2 | PROP  | firstName\x1fk1        | Joseph |

## Publish

| Name          | Value          |                     |
|---------------|----------------|---------------------|
| firstName     | ~~Joe~~ Joseph | ~~Hidden(WS1)~~     | 
| ~~firstName~~ | ~~Joseph~~     | ~~Visibility(WS1)~~ |

        v1.softDeleteProperty("k1", "firstName", V("WS1"))
        v1.addPropertyValue("k1", "firstName", "Joseph", V(""))

| RK | Vis | t | CF    | CQ                     | Value  |
|----|-----|---|-------|------------------------|--------|
| v1 |     | 1 | PROP  | firstName\x1fk1        | Joe    |
| v1 |     | 2 | PROPH | firstName\x1fk1\x1fWS1 |        |
| v1 | WS1 | 2 | PROP  | firstName\x1fk1        | Joseph |
| v1 | WS1 | 3 | PROPD | firstName\x1fk1        |        |
| v1 |     | 4 | PROP  | firstName\x1fk1        | Joseph |

## Undo

| Name          | Value          |                     |
|---------------|----------------|---------------------|
| firstName     | Joe            | ~~Hidden(WS1)~~     |
| ~~firstName~~ | ~~Joseph~~     | ~~Visibility(WS1)~~ |

        v1.softDeleteProperty("k1", "firstName", V("WS1"))
        v1.addPropertyValue("k1", "firstName", "Joe", V(""))

| RK | Vis | t | CF    | CQ                     | Value  |
|----|-----|---|-------|------------------------|--------|
| v1 |     | 1 | PROP  | firstName\x1fk1        | Joe    |
| v1 |     | 2 | PROPH | firstName\x1fk1\x1fWS1 |        |
| v1 | WS1 | 2 | PROP  | firstName\x1fk1        | Joseph |
| v1 | WS1 | 3 | PROPD | firstName\x1fk1        |        |
| v1 |     | 4 | PROP  | firstName\x1fk1        | Joe    |
