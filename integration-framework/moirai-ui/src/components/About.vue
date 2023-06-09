<style scoped>
p {
  line-height: 1.3em;
}
.performers-list {
  line-height: 1.8em;
}
#follow a {
  margin-left: 20px;
}
#follow a img {
  vertical-align: middle;
  max-height: 3em;
}
.caci {
  max-height: 5em;
}
.darkAlternate2BorderColor {
  border-color: var(--v-primary-darken2) !important;
}
.lightAlternate2BorderColor {
  border-color: var(--v-primary-lighten2) !important;
}
</style>
<template>
  <div class="About mx-16">
    <v-card class="mx-auto" color="backdrops lighten-1">
      <v-card-text>
        <div :class="headingClasses">
          MULTI-MODAL OPEN INTERFACES FOR REASONING ARTIFICIAL INTELLIGENCE
        </div>
        <p v-bind:class="paragraphClasses">
          "Rapid comprehension of world events is essential for informing U.S. national security - a
          task that becomes more difficult as the amount of unstructured, multimedia information
          grows exponentially. Humans make sense of events by organizing them into narrative
          structures that occur frequently. These structures are abstracted into schemas, which are
          organized units of knowledge that represent a pattern of memory used in human cognition.
          First-wave, or rule-based, symbolic reasoning, AI systems that incorporated hand-crafted
          schemas were unable to scale when matching those schemas to real-world data. Second-wave,
          or machine learning, AI systems require far too many manually-produced, annotated examples
          as training data for supervised machine learning methods to be practical.
          <br /><br />The Knowledge-directed Artificial Intelligence Reasoning Over Schemas (KAIROS)
          program seeks to develop a schema-based AI system that can identify complex events and
          bring them to the attention of users. KAIROS aims to understand complex events described
          in multimedia inputs by developing a semi-automated system that identifies, links, and
          temporally sequences their subsidiary elements, the participants involved, as well as the
          complex event type. Events of interest either create changes that have significant impact
          on national security or participate in causal chains that produce such impacts." --
          <a
            class="basicanchor"
            href="https://www.darpa.mil/program/knowledge-directed-artificial-intelligence-reasoning-over-schemas"
          >
            KAIROS Program Site</a
          >
          <br />
          <br />Multimodal Open Interfaces for Reasoning by Artificial Intelligence (MOIRAI) is the
          integration framework and access interface for DARPA'S KAIROS program. It collects
          service, data and visualizations for each program performer type under a common access
          portal deployed in the cloud atop scaled graph computing and data streaming technologies.
        </p>
        <div :class="headingClasses">DARPA SPONSOR</div>
        <p v-bind:class="paragraphClasses">
          Eduard Hovy<br /><em>Program Manager</em><br />
          <a
            class="basicanchor"
            href="https://www.darpa.mil/program/knowledge-directed-artificial-intelligence-reasoning-over-schemas"
          >
            KAIROS Program Site</a
          ><br />
          <a class="basicanchor" href="https://www.darpa.mil/">DARPA</a>
        </p>
        <div :class="headingClasses">CONTRIBUTING PERFORMERS</div>
        <p :class="paragraphClasses" class="performers-list">
          Carnegie Mellon University<br />International Business Machines Corporation<br />
          Johns Hopkins University<br />NIST<br />Rensselaer Polytechnic Institute<br />
          Research Foundation for the State University of New York<br />
          University of Pennsylvania Linguistic Data Consortium<br />University of Southern
          California
        </p>
        <hr
          class="mb-6 mx-9"
          :class="
            this.$vuetify.theme.dark ? 'darkAlternate2BorderColor' : 'lightAlternate2BorderColor'
          "
        />
        <div :class="headingClasses">BUILD INFO</div>
        <p :class="paragraphClasses">
          Version: {{ version }}<br />Next Century<br />Subsidiary of CACI<br /><br />Contact:
          <a href="mailto:darren.gemoets@caci.com">darren.gemoets@caci.com</a> or use the
          <v-btn
            text
            href="/support"
            text-transform="none"
            class="px-0 button--text text-none text-decoration-underline font-weight-regular"
          >
            Support
          </v-btn>
          page
        </p>

        <a href="https://www.caci.com/"
          ><img
            class="caci mx-9 mb-3"
            alt="CACI International Logo"
            :src="
              this.$vuetify.theme.dark
                ? require('@/assets/CACI_International_logo__White.png')
                : require('@/assets/CACI_International_logo__Black.png')
            "
        /></a>

        <div id="follow" class="mt-4" :class="paragraphClasses">
          Follow us:
          <a href="https://www.facebook.com/CACIIntl/"
            ><img :src="require('@/assets/iconfinder_facebook_834722.png')" alt="Facebook" />
          </a>
          <a href="https://www.linkedin.com/company/caci-international-inc/"
            ><img :src="require('@/assets/iconfinder_linkedin_287553.png')" alt="LinkedIn" />
          </a>
          <a href="https://www.youtube.com/user/CACIInternational"
            ><img :src="require('@/assets/iconfinder_youtube_986962.png')" alt="youtube" />
          </a>
          <a
            href="https://twitter.com/CACIIntl?ref_src=twsrc%5Egoogle%7Ctwcamp%5Eserp%7Ctwgr%5Eauthor"
          >
            <img :src="require('@/assets/iconfinder_twitter_834708.png')" alt="Twitter" />
          </a>
        </div>
        <p :class="paragraphClasses">
          <em>Website maintained by Next Century/CACI</em>
        </p>
      </v-card-text>
    </v-card>
  </div>
</template>

<script lang="ts">
import { Component, Mixins } from "vue-property-decorator";
import BaseComponent from "../views/BaseComponent.vue";

@Component
export default class About extends Mixins(BaseComponent) {
  private version: string = process.env.VUE_APP_VERSION;
  private aboutClasses: string = " px-9";
  private paragraphClasses: string = "primary--text font-weight-regular mb-7 " + this.aboutClasses;
  get headingClasses(): string {
    return (
      "font-weight-medium mb-2 " +
      (this.$vuetify.theme.dark ? "text--disabled" : "text--secondary") +
      this.aboutClasses
    );
  }
}
</script>
