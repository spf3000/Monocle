const React = require("react");

const CompLibrary = require("../../core/CompLibrary.js");

// const variables = require(process.cwd() + "/variables.js");

const MarkdownBlock = CompLibrary.MarkdownBlock;
const Container = CompLibrary.Container;
const GridBlock = CompLibrary.GridBlock;

class HomeSplash extends React.Component {
  render() {
    const { siteConfig, language = "" } = this.props;
    const { baseUrl, docsUrl } = siteConfig;
    const docsPart = `${docsUrl ? `${docsUrl}/` : ""}`;
    const langPart = `${language ? `${language}/` : ""}`;
    const docUrl = doc => `${baseUrl}${docsPart}${langPart}${doc}`;

    const SplashContainer = props => (
      <div className="homeContainer">
        <div className="homeSplashFade">
          <div className="wrapper homeWrapper">{props.children}</div>
        </div>
      </div>
    );

    const ProjectTitle = () => (
      <h2 className="projectTitle">
          <img className="projectTitleLogo" src={siteConfig.titleIcon} />
        <small>{siteConfig.tagline}</small>
      </h2>
    );

    const PromoSection = props => (
      <div className="section promoSection">
        <div className="promoRow">
          <div className="pluginRowBlock">{props.children}</div>
        </div>
      </div>
    );

    const Button = props => (
      <div className="pluginWrapper buttonWrapper">
        <a className="button" href={props.href} target={props.target}>
          {props.children}
        </a>
      </div>
    );

    return (
      <SplashContainer>
        <div className="inner">
          <ProjectTitle siteConfig={siteConfig} />
          <PromoSection>
            <Button href={siteConfig.apiUrl}>API Docs</Button>
            <Button href={docUrl("modules", language)}>Documentation</Button>
            <Button href={siteConfig.repoUrl}>View on GitHub</Button>
          </PromoSection>
        </div>
      </SplashContainer>
    );
  }
}

class Index extends React.Component {
  render() {
    const { config: siteConfig, language = "" } = this.props;
    const { baseUrl } = siteConfig;

    const organization = "com.github.julien-truffaut";
    const latestVersion = "2.0.3"

    const latestVersionBadge = latestVersion
      .replace("-", "--")
      .replace("_", "__");

    const Block = props => (
      <Container
        padding={["bottom", "top"]}
        id={props.id}
        background={props.background}
      >
        <GridBlock
          align="center"
          contents={props.children}
          layout={props.layout}
        />
      </Container>
    );

    const index = `[![Join the chat at https://gitter.im/optics-dev/Monocle](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/optics-dev/Monocle?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.julien-truffaut/monocle_2.12.svg)](http://search.maven.org/#search|ga|1|com.github.julien-truffaut.monocle)

Monocle is an optics library for Scala (and [Scala.js](https://www.scala-js.org/)) strongly inspired by Haskell [Lens](https://github.com/ekmett/lens).

Optics are a group of purely functional abstractions to manipulate (\`get\`, \`set\`, \`modify\`, ...) immutable objects.

## Getting started

Monocle is published to Maven Central and cross-built for Scala \`2.12\` and \`2.13\` so you can just add the following to your build:

\`\`\`scala
libraryDependencies ++= Seq(
 "${organization}" %% "monocle-core"  % "${latestVersion}",
 "${organization}" %% "monocle-macro" % "${latestVersion}",
)
\`\`\`

If you want to use macro annotations such as \`@Lenses\`, you will also need to include the following for Scala \`2.12\`:

\`\`\`scala
addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.1" cross CrossVersion.full)
\`\`\`

On Scala \`2.13\`, enable the compiler flag \`-Ymacro-annotations\` instead:

\`\`\`scala
scalacOptions in Global += "-Ymacro-annotations"
\`\`\`

## Motivation

Scala already provides getters and setters for case classes but modifying nested objects is verbose which makes code
difficult to understand and reason about. Let's have a look at some examples:

\`\`\`scala mdoc:silent
case class Street(number: Int, name: String)
case class Address(city: String, street: Street)
case class Company(name: String, address: Address)
case class Employee(name: String, company: Company)
\`\`\`

Let's say we have an employee and we need to upper case the first character of his company street name.
Here is how we could write it in vanilla Scala:

\`\`\`scala mdoc:silent
val employee = Employee("john", Company("awesome inc", Address("london", Street(23, "high street"))))
\`\`\`

\`\`\`scala mdoc
employee.copy(
  company = employee.company.copy(
    address = employee.company.address.copy(
      street = employee.company.address.street.copy(
        name = employee.company.address.street.name.capitalize // luckily capitalize exists
      )
    )
  )
)
\`\`\`

As we can see \`copy\` is not convenient to update nested objects because we need to repeat ourselves.
Let's see what could we do with Monocle (type annotations are only added for clarity):

\`\`\`scala mdoc:silent
import monocle.Lens
import monocle.macros.GenLens

val company   : Lens[Employee, Company] = GenLens[Employee](_.company)
val address   : Lens[Company , Address] = GenLens[Company](_.address)
val street    : Lens[Address , Street]  = GenLens[Address](_.street)
val streetName: Lens[Street  , String]  = GenLens[Street](_.name)

company composeLens address composeLens street composeLens streetName
\`\`\`

\`composeLens\` takes two \`Lenses\`, one from \`A\` to \`B\` and another one from \`B\` to \`C\` and creates a third \`Lens\` from \`A\` to \`C\`.
Therefore, after composing \`company\`, \`address\`, \`street\` and \`name\`, we obtain a \`Lens\` from \`Employee\` to \`String\` (the street name).
Now we can use this \`Lens\` issued from the composition to \`modify\` the street name using the function \`capitalize\`:

\`\`\`scala mdoc
(company composeLens address composeLens street composeLens streetName).modify(_.capitalize)(employee)
\`\`\`

Here \`modify\` lifts a function \`String => String\` to a function \`Employee => Employee\`.
It works but it would be clearer if we could zoom into the first character of a \`String\` with a \`Lens\`.
However, we cannot write such a \`Lens\` because \`Lenses\` require the field they are directed at to be *mandatory*.
In our case the first character of a \`String\` is optional as a \`String\` can be empty.
So we need another abstraction that would be a sort of partial \`Lens\`, in Monocle it is called an \`Optional\`.

\`\`\`scala mdoc:silent
import monocle.function.Cons.headOption // to use headOption (an optic from Cons typeclass)
\`\`\`

\`\`\`scala mdoc
(company composeLens address
         composeLens street
         composeLens streetName
         composeOptional headOption).modify(_.toUpper)(employee)
\`\`\`

Similarly to \`composeLens\`, \`composeOptional\` takes two \`Optionals\`, one from \`A\` to \`B\` and another from \`B\` to \`C\` and
creates a third \`Optional\` from \`A\` to \`C\`. All \`Lenses\` can be seen as \`Optionals\` where the optional element to zoom into is always
present, hence composing an \`Optional\` and a \`Lens\` always produces an \`Optional\` (see class [diagram](optics.html) for full inheritance
relation between optics).

Monocle offers various functions and macros to cut the boilerplate even further, here is an example:

\`\`\`scala mdoc
import monocle.macros.syntax.lens._

employee.lens(_.company.address.street.name).composeOptional(headOption).modify(_.toUpper)
\`\`\`

Please consult the [documentation](modules.html) or the [scaladoc](/Monocle/api) for more details and examples.

## Copyright and license

All code is available to you under the MIT license, available [here](http://opensource.org/licenses/mit-license.php).
The design is informed by many other projects, in particular Haskell [Lens](https://github.com/ekmett/lens).

Copyright the maintainers, 2016.

`.trim();

    return (
      <div>
        <HomeSplash siteConfig={siteConfig} language={language} />
        <div className="mainContainer">
          <div className="index">
            <MarkdownBlock>{index}</MarkdownBlock>
          </div>
        </div>
      </div>
    );
  }
}

module.exports = Index;
