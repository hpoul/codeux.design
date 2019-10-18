import hljs from 'highlight.js/lib/highlight';
import javascript from 'highlight.js/lib/languages/javascript';
import shell from 'highlight.js/lib/languages/shell';
import bash from 'highlight.js/lib/languages/bash';
import dart from 'highlight.js/lib/languages/dart';

hljs.registerLanguage('javascript', javascript);
hljs.registerLanguage('shell', shell);
hljs.registerLanguage('bash', bash);
hljs.registerLanguage('dart', dart);

import 'highlight.js/styles/atom-one-light.css';

// import 'highlightjs/styles/github.css';
// import 'highlightjs/styles/default.css';
// import 'highlightjs/styles/atom-one-light.css';

hljs.initHighlightingOnLoad();


// import Prism from 'prismjs';

//
// // Import PrismJS extensions
// import 'prismjs/themes/prism.css';
// import 'prismjs/components/prism-scss';
// import 'prismjs/components/prism-bash';
// import 'prismjs/components/prism-shell-session';
//
// // Import Prism JS
// import 'prismjs/plugins/line-numbers/prism-line-numbers';
// import 'prismjs/plugins/line-numbers/prism-line-numbers.css';
// import 'prismjs/plugins/command-line/prism-command-line';
// import 'prismjs/plugins/command-line/prism-command-line.css';


// window.addEventListener('DOMContentLoaded', (event) => {
//     console.log('init prism.');
//     Prism.highlightAll();
// });

