package main

import (
  "fmt"
  "os"
  "net/http"
  "net/http/cookiejar"
  "net/url"
  "crypto/tls"
  "io/ioutil"
  "encoding/json"
)

type UserMe struct {
  CsrfToken          string
  CurrentWorkspaceId string
}

type Element struct {
  Id string
}

type GenericResult struct {

}

var userMe UserMe
var result GenericResult
var client *http.Client

func main() {
  if len(os.Args) != 4 {
    fmt.Printf("usage: <baseurl> <username> <password>");
    os.Exit(1)
  }
  baseUrl := os.Args[1] // "https://localhost:8889"
  username := os.Args[2]
  password := os.Args[3]

  tr := &http.Transport{
    TLSClientConfig: &tls.Config{
      InsecureSkipVerify: true,
    },
  }
  cookieJar, _ := cookiejar.New(nil)
  client = &http.Client{
    Transport: tr,
    Jar: cookieJar,
  }

  // login
  fmt.Printf("Logging in\n")
  _, err := client.PostForm(baseUrl + "/login", url.Values{"username": {username}, "password": {password}})
  if err != nil {
    fmt.Printf("failed to login: %s", err)
    os.Exit(1)
  }
  fmt.Printf("Logged in\n")

  // get CSRF token
  fmt.Printf("Getting CSRF token and current workspace\n")
  getJson(baseUrl + "/user/me", &userMe)
  fmt.Printf("Got CSRF token %s\n", userMe.CsrfToken)
  fmt.Printf("Got Workspace %s\n", userMe.CurrentWorkspaceId)

  // create a vertex
  newElementValues := url.Values{
    "vertexId": {"V1"},
    "conceptType": {"http://visallo.org/person"},
    "visibilitySource": {""},
    "properties": {`{"properties":[{"propertyKey":"k1", "propertyName":"http://visallo.org#title", "value":"Joe", "visibilitySource":"", "metadataString":""}]}`},
  }
  elem := Element{}
  fmt.Printf("Creating vertex\n")
  postForm(baseUrl + "/vertex/new", newElementValues, &elem)
  fmt.Printf("Created vertex: %s\n", elem.Id)

  // log out
  fmt.Printf("Logging out\n")
  postForm(baseUrl + "/logout", url.Values{}, &result)
  fmt.Printf("Logged out\n")
}

func postForm(url string, data url.Values, target interface{}) {
  data.Set("csrfToken", userMe.CsrfToken);
  data.Set("workspaceId", userMe.CurrentWorkspaceId);

  r, err := client.PostForm(url, data)
  if err != nil {
    fmt.Printf("failed to post %s: %s\n", url, err)
    os.Exit(1)
  }

  defer r.Body.Close()
  body, err := ioutil.ReadAll(r.Body)
  if err != nil {
    fmt.Printf("failed to post %s: %s\n", url, err)
    os.Exit(1)
  }
  //fmt.Printf("body %s", body)

  err = json.Unmarshal(body, target)
  if err != nil {
    fmt.Printf("failed to post %s: %s\n%s\n", url, err, body)
    os.Exit(1)
  }
}

func getJson(url string, target interface{}) {
  r, err := client.Get(url)
  if err != nil {
    fmt.Printf("failed to get %s: %s\n", url, err)
    os.Exit(1)
  }

  defer r.Body.Close()
  body, err := ioutil.ReadAll(r.Body)
  if err != nil {
    fmt.Printf("failed to get %s: %s\n", url, err)
    os.Exit(1)
  }
  //fmt.Printf("body %s", body)

  err = json.Unmarshal(body, target)
  if err != nil {
    fmt.Printf("failed to get %s: %s\n%s\n", url, err, body)
    os.Exit(1)
  }
}